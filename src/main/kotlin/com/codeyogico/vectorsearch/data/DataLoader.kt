package com.codeyogico.vectorsearch.data

import com.codeyogico.vectorsearch.model.Product
import kotlinx.serialization.json.*

object DataLoader {

    fun load(): List<Product> {
        val text = DataLoader::class.java
            .getResourceAsStream("/data/bestbuy.json")
            ?.bufferedReader()?.readText()
            ?: error("bestbuy.json not found in classpath")

        val json = Json { ignoreUnknownKeys = true }
        return json.parseToJsonElement(text).jsonArray
            .take(5000)
            .mapNotNull { el -> try { el.jsonObject.toProduct() } catch (_: Exception) { null } }
    }

    fun categories(products: List<Product>): List<String> =
        products.map { it.category }.distinct().sorted()

    private fun JsonObject.toProduct(): Product? {
        val id = this["objectID"]?.jsonPrimitive?.content ?: return null
        val name = this["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return null
        val category = this["hierarchicalCategories"]?.jsonObject
            ?.get("lvl0")?.jsonPrimitive?.contentOrNull
            ?: this["categories"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
            ?: "Other"
        val price = this["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val description = this["description"]?.jsonPrimitive?.contentOrNull ?: ""
        val brand = this["brand"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val rating = this["rating"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val imageUrl = this["image"]?.jsonPrimitive?.contentOrNull ?: ""

        return Product(id, name, category, price, description, brand, rating, imageUrl)
    }
}
