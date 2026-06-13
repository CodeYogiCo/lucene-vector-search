package com.codeyogico.vectorsearch.embed

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.inference.Predictor
import ai.djl.repository.zoo.Criteria

/**
 * Real sentence embeddings via DJL + ONNX Runtime, using sentence-transformers/all-MiniLM-L6-v2.
 *
 * Produces 384-dim semantic vectors — queries match by meaning, not just shared words.
 * The model (~90MB) is downloaded once on first use and cached locally.
 *
 * The underlying DJL Predictor is not thread-safe, so embed() is synchronized. Fine for a
 * demo; for high throughput you'd pool predictors instead.
 */
object EmbeddingService {

    private const val MODEL_URL =
        "djl://ai.djl.huggingface.onnxruntime/sentence-transformers/all-MiniLM-L6-v2"

    @Volatile
    private var predictor: Predictor<String, FloatArray>? = null

    /** Loads the MiniLM model. Safe to call multiple times; only loads once. */
    @Synchronized
    fun init() {
        if (predictor != null) return
        val criteria = Criteria.builder()
            .setTypes(String::class.java, FloatArray::class.java)
            .optModelUrls(MODEL_URL)
            .optEngine("OnnxRuntime")
            .optTranslatorFactory(TextEmbeddingTranslatorFactory())
            .build()
        predictor = criteria.loadModel().newPredictor()
    }

    /** Embed text into a unit-length vector (so DOT_PRODUCT == cosine similarity). */
    @Synchronized
    fun embed(text: String): FloatArray {
        if (predictor == null) init()
        val vector = predictor!!.predict(text.ifBlank { " " })
        normalize(vector)
        return vector
    }

    fun normalize(vector: FloatArray) {
        val norm = Math.sqrt(vector.sumOf { it.toDouble() * it }).toFloat()
        if (norm > 0f) for (i in vector.indices) vector[i] /= norm
    }
}
