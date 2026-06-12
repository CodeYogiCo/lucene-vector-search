package com.codeyogico.vectorsearch

import com.codeyogico.vectorsearch.data.BestBuyProducts
import com.codeyogico.vectorsearch.embed.EmbeddingService
import com.codeyogico.vectorsearch.index.ProductIndexer
import com.codeyogico.vectorsearch.index.ProductSearcher
import org.apache.lucene.store.ByteBuffersDirectory
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VectorSearchTest {

    private lateinit var searcher: ProductSearcher

    @BeforeAll
    fun buildIndex() {
        val dir = ByteBuffersDirectory()
        ProductIndexer(dir).use { indexer ->
            BestBuyProducts.catalog.forEach { indexer.index(it) }
            indexer.commit()
        }
        searcher = ProductSearcher(dir)
    }

    // --- Embedding ---

    @Test
    fun `embedding produces unit vector`() {
        val vec = EmbeddingService.embed("wireless noise cancelling headphones")
        val norm = Math.sqrt(vec.sumOf { it.toDouble() * it }).toFloat()
        assertTrue(Math.abs(norm - 1.0f) < 1e-5f, "Expected unit vector, got norm=$norm")
    }

    @Test
    fun `similar texts produce closer embeddings than dissimilar ones`() {
        val a = EmbeddingService.embed("Sony noise cancelling headphones")
        val b = EmbeddingService.embed("Bose noise cancelling headphones")
        val c = EmbeddingService.embed("Samsung 4K OLED television")

        fun dot(x: FloatArray, y: FloatArray) = x.zip(y.toTypedArray()).sumOf { (xi, yi) -> xi.toDouble() * yi }

        val simAB = dot(a, b)
        val simAC = dot(a, c)
        assertTrue(simAB > simAC, "Expected headphone texts to be closer than headphone vs TV")
    }

    // --- Vector search ---

    @Test
    fun `vector search returns requested number of results`() {
        val results = searcher.vectorSearch("4K OLED television", k = 5)
        assertEquals(5, results.size)
    }

    @Test
    fun `vector search scores are non-negative`() {
        val results = searcher.vectorSearch("gaming laptop RTX")
        assertTrue(results.all { it.score >= 0f })
    }

    // --- Filtered vector search ---

    @Test
    fun `filtered vector search returns only the requested category`() {
        val results = searcher.filteredVectorSearch("wireless audio", category = "headphones", k = 10)
        assertFalse(results.isEmpty(), "Expected at least one headphone result")
        assertTrue(results.all { it.product.category == "headphones" },
            "All results should be in the headphones category")
    }

    @Test
    fun `filtered vector search excludes other categories even with generic query`() {
        val results = searcher.filteredVectorSearch("device", category = "cameras", k = 10)
        assertTrue(results.all { it.product.category == "cameras" })
    }

    @Test
    fun `filtered search with narrow category returns fewer than k when catalog is small`() {
        // Only 3 camera products in the catalog
        val results = searcher.filteredVectorSearch("professional photo", category = "cameras", k = 10)
        assertTrue(results.size <= 3)
    }

    // --- BM25 text search ---

    @Test
    fun `text search finds products by brand name`() {
        val results = searcher.textSearch("Apple MacBook")
        assertTrue(results.any { it.product.brand == "Apple" },
            "Expected at least one Apple product in text search results")
    }

    // --- Hybrid search ---

    @Test
    fun `hybrid search returns results`() {
        val results = searcher.hybridSearch("noise cancelling headphones")
        assertFalse(results.isEmpty())
    }

    @Test
    fun `hybrid search surfaces headphones for headphone query`() {
        val results = searcher.hybridSearch("noise cancelling wireless headphones", k = 5)
        val headphoneCount = results.count { it.product.category == "headphones" }
        assertTrue(headphoneCount >= 2, "Expected at least 2 headphones in top-5 hybrid results, got $headphoneCount")
    }

    @Test
    fun `hybrid search result scores are non-negative`() {
        val results = searcher.hybridSearch("OLED 4K TV Samsung")
        assertTrue(results.all { it.score >= 0f })
    }

    // --- Filtered hybrid search ---

    @Test
    fun `filtered hybrid search restricts to category`() {
        val results = searcher.filteredHybridSearch("best product", category = "gaming", k = 10)
        assertTrue(results.all { it.product.category == "gaming" },
            "Filtered hybrid should return only gaming products")
    }

    @Test
    fun `filtered hybrid surfaces relevant product within category`() {
        val results = searcher.filteredHybridSearch("PlayStation console", category = "gaming", k = 5)
        assertTrue(results.any { it.product.id == "BB031" },
            "Expected PS5 (BB031) in filtered hybrid gaming results")
    }
}
