package com.codeyogico.vectorsearch

import com.codeyogico.vectorsearch.data.DataLoader
import com.codeyogico.vectorsearch.index.ProductIndexer
import com.codeyogico.vectorsearch.index.ProductSearcher
import com.codeyogico.vectorsearch.server.startSearchServer
import org.apache.lucene.store.ByteBuffersDirectory

fun main() {
    val directory = ByteBuffersDirectory()

    println("Loading Best Buy dataset...")
    val products = DataLoader.load()
    val categories = DataLoader.categories(products)
    println("Loaded ${products.size} products across ${categories.size} categories.")

    println("Building index...")
    ProductIndexer(directory).use { indexer ->
        products.forEach { indexer.index(it) }
        indexer.commit()
    }
    println("Index ready. Starting server on http://localhost:8080")

    startSearchServer(ProductSearcher(directory), categories)
}
