package com.codeyogico.vectorsearch.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val brand: String,
    val rating: Double,
    val imageUrl: String = "",
)

@Serializable
data class SearchResult(
    val product: Product,
    val score: Float,
    // HTML with <mark> around BM25-matched terms; null for pure vector search.
    val nameHighlight: String? = null,
    val descriptionHighlight: String? = null,
    // Where this hit came from: "vector", "bm25", or "both" (hybrid).
    val source: String? = null,
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult>,
    val totalHits: Int,
    val mode: String,
    val durationMs: Long,
)
