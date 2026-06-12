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
)

@Serializable
data class SearchResult(
    val product: Product,
    val score: Float,
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult>,
    val totalHits: Int,
    val mode: String,
    val durationMs: Long,
)
