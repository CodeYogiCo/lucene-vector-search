# lucene-vector-search

Vector search over a Best Buy product catalog using Lucene's built-in HNSW graph — no separate vector database.

Companion code for [You already have a vector database](https://codeyogico.github.io/posts/vector-search-lucene-is-all-you-need/).

---

## The idea

Lucene has had HNSW-based vector search since version 9.0. Most teams missed it because they were already reaching for something newer-looking.

The key advantage over purpose-built vector databases is **filtered search**. When you search within a category, Lucene hands the category predicate directly to the HNSW graph traversal — nodes that don't match are invisible to the walk, not fetched and discarded afterward. This works correctly at 0.1% selectivity the same way it works at 50%.

You also get hybrid BM25 + vector search in a single `BooleanQuery` — two signals, one round-trip, no external merge layer.

---

## Stack

| Layer | Technology |
|-------|-----------|
| Vector index | Apache Lucene 9.10 (HNSW via `KnnFloatVectorField`) |
| Search modes | Vector-only, filtered vector, BM25, hybrid BM25+vector |
| Embedding | Hash-based 128-dim (offline, no model download — swap for DJL/OpenAI) |
| API server | Ktor 2.3 on Netty |
| UI | Vanilla JS + Tailwind CSS |
| Language | Kotlin 1.9 / JVM 17 |
| Build | Gradle with Shadow plugin (fat JAR) |

---

## Project structure

```
src/main/kotlin/com/codeyogico/vectorsearch/
  Main.kt                    — boot: build index → start server
  model/Product.kt           — Product, SearchResult, SearchResponse
  embed/EmbeddingService.kt  — text → float[] (swap for any real model)
  data/BestBuyProducts.kt    — 33 sample products across 8 categories
  index/ProductIndexer.kt    — writes KnnFloatVectorField + stored fields
  index/ProductSearcher.kt   — vector, filtered, BM25, hybrid search
  server/SearchServer.kt     — Ktor routes + static file serving

src/main/resources/static/index.html  — search UI
src/test/kotlin/.../VectorSearchTest.kt — 13 JUnit 5 tests
```

---

## Run locally

**Requirements:** JDK 17+

```bash
git clone https://github.com/CodeYogiCo/lucene-vector-search
cd lucene-vector-search
./gradlew run
```

Open [http://localhost:8080](http://localhost:8080).

---

## Run tests

```bash
./gradlew test
```

13 tests covering embedding correctness, all four search modes, and category filtering.

---

## API

```
GET /api/search
  ?q=        search query (required)
  &category= filter by category (optional)
  &mode=     hybrid | vector | text  (default: hybrid)
  &limit=    1–50 (default: 10)

GET /api/categories
  returns the list of available categories
```

**Example:**
```bash
curl "http://localhost:8080/api/search?q=noise+cancelling+headphones&mode=hybrid&limit=5"
```

**Search modes:**

| `mode` | What Lucene does |
|--------|-----------------|
| `vector` | Pure HNSW traversal |
| `vector` + `category` | HNSW with category bitset handed to the graph walk |
| `text` | BM25 over name + description |
| `hybrid` | `BooleanQuery(SHOULD vector, SHOULD BM25)` — one round-trip |

---

## Deploy to Railway

1. Sign in at [railway.app](https://railway.app) with GitHub
2. **New Project → Deploy from GitHub repo** → select `lucene-vector-search`
3. Railway picks up `nixpacks.toml` and builds automatically
4. **Settings → Networking → Generate Domain** to get a public URL

Every `git push` to `main` triggers a redeploy. No env vars needed.

---

## Swapping the embedding model

`EmbeddingService.kt` is the only place that produces embeddings. Replace `embed()` with any model — the indexer and searcher are unchanged:

```kotlin
// Drop-in replacement using DJL + HuggingFace all-MiniLM-L6-v2
fun embed(text: String): FloatArray {
    val criteria = Criteria.builder()
        .setTypes(String::class.java, FloatArray::class.java)
        .optModelUrls("djl://ai.djl.huggingface.pytorch/all-MiniLM-L6-v2")
        .build()
    return criteria.loadModel().newPredictor().predict(text)
}
```

Change `DIMS` in `EmbeddingService` to match the model output dimension and rebuild the index.

---

## Directory abstraction

Lucene's `Directory` is the only seam between the index and storage. Swap `ByteBuffersDirectory` (in-memory) for any implementation:

```kotlin
// Local disk
val directory = FSDirectory.open(Path.of("/var/index"))

// S3 / GCS / Azure Blob — community implementations available
// Segments are write-once, so any put-once store works natively
```
