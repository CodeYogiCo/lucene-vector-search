# lucene-vector-search

**LuceneShop** — semantic, keyword, and hybrid product search built entirely on Apache Lucene's HNSW vector index. No separate vector database.

Companion code for [You already have a vector database](https://codeyogico.github.io/posts/vector-search-lucene-is-all-you-need/).

---

## The idea

Lucene has had HNSW-based vector search since version 9.0. Most teams missed it because they were already reaching for something newer-looking.

Two things make it enough on its own for most teams:

- **Filtered search done right.** When you search within a category, Lucene hands the predicate directly to the HNSW graph traversal — non-matching nodes are invisible to the walk, not fetched and discarded afterward. This works at 0.1% selectivity the same way it works at 50%, where naive post-filtering falls apart.
- **Hybrid in one pass.** Vector and BM25 run inside a single `BooleanQuery` — two signals, one round-trip, no external merge layer you own.

This repo is a working demo of both, over ~1,000 real products, with genuine sentence embeddings.

---

## What it does

- **Three search modes** — Vector (meaning), BM25 (keywords), and Hybrid (both, blended).
- **Real semantic embeddings** — `sentence-transformers/all-MiniLM-L6-v2` running in-process via DJL + ONNX Runtime (384-dim). "comfy earbuds" finds headphones with no shared words.
- **Filtered search** — a category filter handed to the HNSW traversal (and to BM25/hybrid), never post-filtered.
- **Source attribution** — in hybrid mode each result is tagged `vector`, `bm25`, or `both`, computed per-result via Lucene `explain()`.
- **Match highlighting** — BM25-matched terms are wrapped in `<mark>` and shown highlighted.
- **Product details page** and **example-query chips** for discovery.

---

## Stack

| Layer | Technology |
|-------|-----------|
| Vector index | Apache Lucene 10.4 (HNSW via `KnnFloatVectorField`, SIMD vector scoring) |
| Embedding | DJL + ONNX Runtime — all-MiniLM-L6-v2, 384-dim |
| Search | Vector / filtered vector / BM25 / hybrid, with highlighting + source attribution |
| API server | Ktor 2.3 on Netty |
| UI | Vanilla JS + Tailwind CSS |
| Data | ~1,000 products from the public [Algolia ecommerce dataset](https://github.com/algolia/datasets) |
| Language | Kotlin 1.9 / JVM 21 |
| Build | Gradle with the Shadow plugin (fat JAR) |

---

## Project structure

```
src/main/kotlin/com/codeyogico/vectorsearch/
  Main.kt                    — boot: load model → build index → start server
  model/Product.kt           — Product, SearchResult (+ highlights, source), SearchResponse
  embed/EmbeddingService.kt  — MiniLM embeddings via DJL + ONNX Runtime
  data/DataLoader.kt         — parses the bundled product JSON
  index/ProductIndexer.kt    — writes KnnFloatVectorField + searchable/stored fields
  index/ProductSearcher.kt   — vector / filtered / BM25 / hybrid, highlight + source
  server/SearchServer.kt     — Ktor routes, static serving, async-ready health gate

src/main/resources/
  data/bestbuy.json          — product catalog
  static/index.html          — search UI (modes, filters, chips, explainers)
  static/product.html        — product details page

src/test/kotlin/.../VectorSearchTest.kt — JUnit 5 tests
```

---

## Run locally

**Requirements:** JDK 21+ (Lucene 10 requires Java 21)

```bash
git clone https://github.com/CodeYogiCo/lucene-vector-search
cd lucene-vector-search
./gradlew run
```

Open [http://localhost:8080](http://localhost:8080).

On first run the MiniLM model (~90MB) is downloaded and cached locally; subsequent starts are fast. The HTTP server binds immediately and search endpoints return `503` until the index is ready (the `/health` endpoint reports `indexing` vs `ready`).

---

## Run tests

```bash
./gradlew test
```

Covers embedding properties, all search modes, category filtering, and lookup by id. (The test run downloads the MiniLM model on first execution.)

---

## API

```
GET /api/search
  ?q=        search query (required)
  &category= filter by category (optional)
  &mode=     hybrid | vector | text   (default: hybrid)
  &limit=    1–50 (default: 10)

GET /api/categories     → list of categories in the catalog
GET /api/product/{id}   → full product by id (for the details page)
GET /health             → { "status": "ready" | "indexing" }
```

**Example:**
```bash
curl "http://localhost:8080/api/search?q=noise+cancelling+headphones&mode=hybrid&limit=5"
```

**Search modes:**

| `mode` | What Lucene does |
|--------|-----------------|
| `vector` | Pure HNSW traversal over MiniLM embeddings |
| `vector` + `category` | HNSW with the category bitset handed to the graph walk |
| `text` | BM25 over name + description, matched terms highlighted |
| `hybrid` | `BooleanQuery(SHOULD vector, SHOULD BM25)` — one round-trip, each hit tagged with its source |

---

## Embeddings

`EmbeddingService.kt` is the only place that produces vectors. It loads MiniLM once and embeds text to a unit-length 384-dim vector (so `DOT_PRODUCT` == cosine):

```kotlin
val criteria = Criteria.builder()
    .setTypes(String::class.java, FloatArray::class.java)
    .optModelUrls("djl://ai.djl.huggingface.onnxruntime/sentence-transformers/all-MiniLM-L6-v2")
    .optEngine("OnnxRuntime")
    .optTranslatorFactory(TextEmbeddingTranslatorFactory())
    .build()
```

To use a different model, change the model URL (and the index dimension follows the vector length automatically). Both indexing and queries go through the same method, so they always share an embedding space.

---

## Storage: the `Directory` seam

A Lucene index is reached only through its `Directory` abstraction. This demo uses an in-memory directory and rebuilds on startup:

```kotlin
val directory = ByteBuffersDirectory()      // in-memory, rebuilt each boot
// val directory = FSDirectory.open(Path.of("/var/index"))   // local disk, persistent
```

Swapping the `Directory` is the only change needed to move where segments live — the indexer and searcher are untouched.

---

## Storing segments in S3 / object storage (design notes — not implemented)

A Lucene index is not one mutable file. It's a set of **segments**, each one complete, immutable, and write-once, plus a small `segments_N` commit point naming the live ones:

```
index/
  _0.cfs        segment 0 — docs, terms, vectors, HNSW graph
  _1.cfs        segment 1
  segments_3    commit point (which segments are live)
```

New documents create new segments; merges combine small ones into larger ones; nothing is ever overwritten. That write-once shape is exactly what object stores (S3, GCS, Azure Blob) are built for — so a large HNSW graph can live in cheap object storage instead of attached disk, with no locks and no partial-write reconciliation to coordinate.

### Approach A — sync to local disk on start (simplest)

Treat S3 as the durable home of the index and local disk as a cache.

1. **Build once, offline.** A separate indexing job writes the index to an `FSDirectory`, commits, then uploads the segment files to a bucket prefix (e.g. `s3://my-bucket/index/v1/`). Because files are immutable, the upload is a plain put-once; no special ordering is needed beyond writing `segments_N` last.
2. **On app start, download then open.** The service syncs the prefix to a local directory and opens it normally:

   ```kotlin
   // pseudocode
   s3.syncPrefixToDir("s3://my-bucket/index/v1/", localPath)   // skips files already cached
   val directory = FSDirectory.open(localPath)
   val searcher  = ProductSearcher(directory)
   ```
3. **Updates = new prefix.** Publish a new immutable version (`index/v2/`) and flip the pointer the app reads at boot. Old versions stay valid until you delete them.

Trade-off: the full index must fit on local disk, and cold start pays the download cost once.

### Approach B — read segments directly from S3 (streaming, with caching)

Implement a custom `Directory` whose `IndexInput` fetches byte ranges from S3 on demand and caches hot regions (e.g. the HNSW graph) on local SSD. The app starts without downloading the whole index; only the bytes a query touches are fetched, then cached.

- Lucene's `Directory` / `IndexInput` is the entire interface you implement; there are community S3/GCS directory implementations to start from, or you can wrap an existing `MMapDirectory` cache in front of range requests.
- Segments being immutable is what makes this safe: a cached byte range can never go stale, so caching needs no invalidation.

Trade-off: more moving parts and per-query latency depends on cache warmth; in return the local node needs only enough disk for the working set, not the whole index.

### Why this composes well

| Property | Consequence for object storage |
|----------|-------------------------------|
| Segments are immutable | Put-once writes; cache forever, no invalidation |
| Commit point names live segments | Atomic version flips by swapping a prefix/pointer |
| `Directory` is the only storage seam | No change to indexing or query code |
| One write = embedding + doc + commit | No second system to drift out of sync |

For most teams the filtered HNSW is already there and the segments already fit object storage — the threshold for needing a dedicated vector database is higher than it looks.
