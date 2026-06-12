package com.codeyogico.vectorsearch.index

import com.codeyogico.vectorsearch.embed.EmbeddingService
import com.codeyogico.vectorsearch.model.Product
import com.codeyogico.vectorsearch.model.SearchResult
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory

class ProductSearcher(directory: Directory) : AutoCloseable {

    private val reader = DirectoryReader.open(directory)
    private val searcher = IndexSearcher(reader)
    private val analyzer = StandardAnalyzer()

    /**
     * Pure HNSW vector search — no text signals.
     */
    fun vectorSearch(query: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        return search(KnnFloatVectorQuery("embedding", vec, k), k)
    }

    /**
     * Filtered HNSW — the category predicate is handed to the graph traversal.
     * Nodes that don't pass the filter are invisible to the walk; not fetched, not discarded.
     * Correct at 0.1% selectivity exactly the same as at 50%.
     */
    fun filteredVectorSearch(query: String, category: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        val filter = TermQuery(Term("category", category))
        return search(KnnFloatVectorQuery("embedding", vec, k, filter), k)
    }

    /**
     * BM25 text search only — for comparison.
     */
    fun textSearch(query: String, category: String = "", k: Int = 10): List<SearchResult> {
        val textQuery = QueryParser("name", analyzer).parse(QueryParser.escape(query))
        val descQuery = QueryParser("description", analyzer).parse(QueryParser.escape(query))
        val builder = BooleanQuery.Builder()
            .add(textQuery, BooleanClause.Occur.SHOULD)
            .add(descQuery, BooleanClause.Occur.SHOULD)
        if (category.isNotEmpty())
            builder.add(TermQuery(Term("category", category)), BooleanClause.Occur.MUST)
        return search(builder.build(), k)
    }

    /**
     * Hybrid: vector + BM25 in a single BooleanQuery.
     * Two signals, one round-trip — no external merge layer.
     */
    fun hybridSearch(query: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        val vectorQuery = KnnFloatVectorQuery("embedding", vec, k)
        val textQuery = QueryParser("name", analyzer).parse(QueryParser.escape(query))
        val descQuery = QueryParser("description", analyzer).parse(QueryParser.escape(query))

        val hybrid = BooleanQuery.Builder()
            .add(vectorQuery, BooleanClause.Occur.SHOULD)
            .add(textQuery, BooleanClause.Occur.SHOULD)
            .add(descQuery, BooleanClause.Occur.SHOULD)
            .build()

        return search(hybrid, k)
    }

    /**
     * Filtered hybrid — category filter on the HNSW traversal, BM25 fused in the same pass.
     */
    fun filteredHybridSearch(query: String, category: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        val filter = TermQuery(Term("category", category))
        val vectorQuery = KnnFloatVectorQuery("embedding", vec, k, filter)
        val textQuery = QueryParser("name", analyzer).parse(QueryParser.escape(query))

        val hybrid = BooleanQuery.Builder()
            .add(vectorQuery, BooleanClause.Occur.SHOULD)
            .add(textQuery, BooleanClause.Occur.SHOULD)
            .add(filter, BooleanClause.Occur.MUST)
            .build()

        return search(hybrid, k)
    }

    private fun search(query: Query, k: Int): List<SearchResult> {
        val topDocs = searcher.search(query, k)
        val storedFields = searcher.storedFields()
        return topDocs.scoreDocs.map { scoreDoc ->
            val doc = storedFields.document(scoreDoc.doc)
            SearchResult(
                product = Product(
                    id = doc.get("id"),
                    name = doc.get("name"),
                    category = doc.get("category"),
                    price = doc.getField("price").numericValue().toDouble(),
                    description = doc.get("description"),
                    brand = doc.get("brand"),
                    rating = doc.get("rating").toDouble(),
                ),
                score = scoreDoc.score,
            )
        }
    }

    override fun close() = reader.close()
}
