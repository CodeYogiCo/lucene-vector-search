package com.codeyogico.vectorsearch

import com.codeyogico.vectorsearch.data.DataLoader
import com.codeyogico.vectorsearch.embed.EmbeddingService
import com.codeyogico.vectorsearch.index.ProductIndexer
import com.codeyogico.vectorsearch.index.ProductSearcher
import com.codeyogico.vectorsearch.server.AppState
import com.codeyogico.vectorsearch.server.startSearchServer
import org.apache.lucene.store.ByteBuffersDirectory

fun main() {
    val state = AppState()

    // Start HTTP server first — Railway health checks need a response immediately.
    // Search endpoints return 503 until the index is ready.
    startSearchServer(state)

    println("Loading MiniLM embedding model (first run downloads ~90MB)...")
    EmbeddingService.init()

    println("Loading dataset...")
    val products = DataLoader.load()
    println("Loaded ${products.size} products. Embedding + indexing...")

    val directory = ByteBuffersDirectory()
    ProductIndexer(directory).use { indexer ->
        products.forEach { indexer.index(it) }
        indexer.commit()
    }

    state.categories = DataLoader.categories(products)
    state.searcher = ProductSearcher(directory)
    state.ready = true
    println("Index ready — ${products.size} products across ${state.categories.size} categories.")

    Thread.currentThread().join() // keep main thread alive
}
