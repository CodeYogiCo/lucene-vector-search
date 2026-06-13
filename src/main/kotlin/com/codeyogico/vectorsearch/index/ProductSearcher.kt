package com.codeyogico.vectorsearch.index

import com.codeyogico.vectorsearch.embed.EmbeddingService
import com.codeyogico.vectorsearch.model.Product
import com.codeyogico.vectorsearch.model.SearchResult
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.search.highlight.NullFragmenter
import org.apache.lucene.search.highlight.QueryScorer
import org.apache.lucene.search.highlight.SimpleHTMLFormatter
import org.apache.lucene.store.Directory

class ProductSearcher(directory: Directory) : AutoCloseable {

    private val reader = DirectoryReader.open(directory)
    private val searcher = IndexSearcher(reader)
    private val analyzer = StandardAnalyzer()
    private val formatter = SimpleHTMLFormatter("<mark>", "</mark>")

    /** Pure HNSW vector search — no text signals, no highlighting. */
    fun vectorSearch(query: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        return search(KnnFloatVectorQuery("embedding", vec, k), k, null, staticSource = "vector")
    }

    /**
     * Filtered HNSW — the category predicate is handed to the graph traversal.
     * Nodes that don't pass the filter are invisible to the walk; not fetched, not discarded.
     */
    fun filteredVectorSearch(query: String, category: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        val filter = TermQuery(Term("category", category))
        return search(KnnFloatVectorQuery("embedding", vec, k, filter), k, null, staticSource = "vector")
    }

    /** BM25 text search over name + description, with matched-term highlighting. */
    fun textSearch(query: String, category: String = "", k: Int = 10): List<SearchResult> {
        val text = textQuery(query)
        val builder = BooleanQuery.Builder().add(text, BooleanClause.Occur.SHOULD)
        if (category.isNotEmpty()) {
            builder.add(TermQuery(Term("category", category)), BooleanClause.Occur.MUST)
            // Without this, SHOULD clauses become optional the moment a MUST clause exists —
            // every document in the category is returned even with zero text match.
            builder.setMinimumNumberShouldMatch(1)
        }
        return search(builder.build(), k, text, staticSource = "bm25")
    }

    /** Hybrid: vector + BM25 in a single BooleanQuery. Two signals, one round-trip. */
    fun hybridSearch(query: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        // Give the vector clause real recall (a candidate pool), not just the final k —
        // otherwise BM25 winners rarely fall inside the vector top-k and everything
        // looks like a BM25-only match.
        val vector = KnnFloatVectorQuery("embedding", vec, candidateK(k))
        val text = textQuery(query)
        val hybrid = BooleanQuery.Builder()
            .add(vector, BooleanClause.Occur.SHOULD)
            .add(text, BooleanClause.Occur.SHOULD)
            .build()
        return search(hybrid, k, text, vectorComponent = vector, textComponent = text)
    }

    /** Filtered hybrid — category filter on the HNSW traversal, BM25 fused in the same pass. */
    fun filteredHybridSearch(query: String, category: String, k: Int = 10): List<SearchResult> {
        val vec = EmbeddingService.embed(query)
        val filter = TermQuery(Term("category", category))
        val vector = KnnFloatVectorQuery("embedding", vec, candidateK(k), filter)
        val text = textQuery(query)
        val hybrid = BooleanQuery.Builder()
            .add(vector, BooleanClause.Occur.SHOULD)
            .add(text, BooleanClause.Occur.SHOULD)
            .add(filter, BooleanClause.Occur.MUST)
            .build()
        return search(hybrid, k, text, vectorComponent = vector, textComponent = text)
    }

    private fun candidateK(k: Int) = maxOf(k, 100)

    /** Fetch a single product by id, for the details page. */
    fun getById(id: String): Product? {
        val top = searcher.search(TermQuery(Term("id", id)), 1)
        if (top.scoreDocs.isEmpty()) return null
        return toProduct(searcher.storedFields().document(top.scoreDocs[0].doc))
    }

    // name + description as a single SHOULD-of-SHOULD text query, reused for search and highlighting
    private fun textQuery(query: String): Query {
        val escaped = QueryParser.escape(query)
        return BooleanQuery.Builder()
            .add(QueryParser("name", analyzer).parse(escaped), BooleanClause.Occur.SHOULD)
            .add(QueryParser("description", analyzer).parse(escaped), BooleanClause.Occur.SHOULD)
            .build()
    }

    private fun search(
        query: Query,
        k: Int,
        highlightQuery: Query?,
        staticSource: String? = null,
        vectorComponent: Query? = null,
        textComponent: Query? = null,
    ): List<SearchResult> {
        val topDocs = searcher.search(query, k)
        val storedFields = searcher.storedFields()
        return topDocs.scoreDocs.map { scoreDoc ->
            val doc = storedFields.document(scoreDoc.doc)
            SearchResult(
                product = toProduct(doc),
                score = scoreDoc.score,
                nameHighlight = highlightQuery?.let { highlight("name", doc.get("name") ?: "", it) },
                descriptionHighlight = highlightQuery?.let { highlight("description", doc.get("description") ?: "", it) },
                source = sourceOf(scoreDoc.doc, staticSource, vectorComponent, textComponent),
            )
        }
    }

    // In hybrid mode, ask each component query whether it matched this doc (via explain).
    // For single-mode searches there's only one source, so use the static label.
    private fun sourceOf(docId: Int, staticSource: String?, vector: Query?, text: Query?): String? {
        if (vector == null || text == null) return staticSource
        val matchedVector = runCatching { searcher.explain(vector, docId).isMatch }.getOrDefault(false)
        val matchedText = runCatching { searcher.explain(text, docId).isMatch }.getOrDefault(false)
        return when {
            matchedVector && matchedText -> "both"
            matchedVector -> "vector"
            matchedText -> "bm25"
            else -> staticSource
        }
    }

    // Re-analyzes the stored text and wraps matched terms in <mark>; null if nothing matched.
    private fun highlight(field: String, text: String, query: Query): String? {
        if (text.isBlank()) return null
        return try {
            val highlighter = Highlighter(formatter, QueryScorer(query, field))
            highlighter.textFragmenter = NullFragmenter() // highlight the whole field, don't fragment
            highlighter.getBestFragment(analyzer, field, text)
        } catch (_: Exception) {
            null
        }
    }

    private fun toProduct(doc: Document) = Product(
        id = doc.get("id") ?: "",
        name = doc.get("name") ?: "",
        category = doc.get("category") ?: "",
        price = doc.getField("price")?.numericValue()?.toDouble() ?: 0.0,
        description = doc.get("description") ?: "",
        brand = doc.get("brand") ?: "",
        rating = doc.getField("rating")?.numericValue()?.toDouble() ?: 0.0,
        imageUrl = doc.get("imageUrl") ?: "",
    )

    override fun close() = reader.close()
}
