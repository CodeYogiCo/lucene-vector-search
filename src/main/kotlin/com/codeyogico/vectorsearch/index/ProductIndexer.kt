package com.codeyogico.vectorsearch.index

import com.codeyogico.vectorsearch.embed.EmbeddingService
import com.codeyogico.vectorsearch.model.Product
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.VectorSimilarityFunction
import org.apache.lucene.store.Directory

class ProductIndexer(directory: Directory) : AutoCloseable {

    private val writer = IndexWriter(directory, IndexWriterConfig(StandardAnalyzer()))

    fun index(product: Product) {
        val embedding = EmbeddingService.embed("${product.name} ${product.description} ${product.brand}")

        val doc = Document().apply {
            add(StringField("id", product.id, Field.Store.YES))
            add(TextField("name", product.name, Field.Store.YES))
            add(StringField("category", product.category, Field.Store.YES))
            add(DoubleDocValuesField("price", product.price))
            add(StoredField("price", product.price))
            add(StoredField("description", product.description))
            add(StoredField("brand", product.brand))
            add(StoredField("rating", product.rating))
            add(StoredField("imageUrl", product.imageUrl))
            add(KnnFloatVectorField("embedding", embedding, VectorSimilarityFunction.DOT_PRODUCT))
        }

        writer.addDocument(doc)
    }

    fun commit() = writer.commit()

    override fun close() = writer.close()
}
