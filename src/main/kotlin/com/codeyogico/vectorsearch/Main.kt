package com.codeyogico.vectorsearch

import com.codeyogico.vectorsearch.data.BestBuyProducts
import com.codeyogico.vectorsearch.index.ProductIndexer
import com.codeyogico.vectorsearch.index.ProductSearcher
import com.codeyogico.vectorsearch.server.startSearchServer
import org.apache.lucene.store.ByteBuffersDirectory

fun main() {
    // In-memory directory — swap for FSDirectory.open(path) to persist to disk,
    // or any Directory implementation that wraps S3/GCS for object storage.
    val directory = ByteBuffersDirectory()

    println("Indexing ${BestBuyProducts.catalog.size} Best Buy products...")
    ProductIndexer(directory).use { indexer ->
        BestBuyProducts.catalog.forEach { indexer.index(it) }
        indexer.commit()
    }
    println("Index ready. Starting server on http://localhost:8080")

    val searcher = ProductSearcher(directory)
    startSearchServer(searcher)
}
