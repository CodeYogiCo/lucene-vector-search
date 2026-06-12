package com.codeyogico.vectorsearch.embed

/**
 * Offline hash-based text embedding.
 *
 * Produces a 128-dim unit vector using word unigrams, bigrams, and character
 * trigrams — good enough to demonstrate Lucene HNSW without a model download.
 * Swap embed() for any real model (DJL, OpenAI, etc.) — the index and search
 * code is unchanged.
 */
object EmbeddingService {

    const val DIMS = 128

    fun embed(text: String): FloatArray {
        val tokens = tokenize(text)
        val vector = FloatArray(DIMS)

        for (token in tokens) addHash(vector, token.hashCode(), 1.0f)

        for (i in 0 until tokens.size - 1)
            addHash(vector, (tokens[i] + "_" + tokens[i + 1]).hashCode(), 0.5f)

        val flat = tokens.joinToString(" ")
        for (i in 0..flat.length - 3)
            addHash(vector, flat.substring(i, i + 3).hashCode(), 0.2f)

        normalize(vector)
        return vector
    }

    private fun tokenize(text: String) =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }

    private fun addHash(vector: FloatArray, hash: Int, weight: Float) {
        val primes = intArrayOf(1, 3, 7, 13, 23, 43, 83)
        for (prime in primes) {
            val idx = Math.abs((hash.toLong() * prime) % DIMS).toInt()
            vector[idx] += if ((hash xor prime) >= 0) weight else -weight
        }
    }

    fun normalize(vector: FloatArray) {
        val norm = Math.sqrt(vector.sumOf { it.toDouble() * it }).toFloat()
        if (norm > 0f) vector.indices.forEach { vector[it] /= norm }
    }
}
