package com.codeyogico.vectorsearch.server

import com.codeyogico.vectorsearch.index.ProductSearcher
import com.codeyogico.vectorsearch.model.SearchResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

class AppState {
    @Volatile var ready = false
    @Volatile var searcher: ProductSearcher? = null
    @Volatile var categories: List<String> = emptyList()
}

fun startSearchServer(state: AppState, port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080) {
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; ignoreUnknownKeys = true })
        }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
        }

        routing {
            staticResources("/", "static") {
                default("index.html")
            }

            // Railway health check — always 200, even while indexing
            get("/health") {
                call.respond(mapOf("status" to if (state.ready) "ready" else "indexing"))
            }

            get("/api/search") {
                if (!state.ready) {
                    call.respond(HttpStatusCode.ServiceUnavailable,
                        mapOf("error" to "Index is being built, please try again in a few seconds"))
                    return@get
                }

                val searcher = state.searcher!!
                val q = call.request.queryParameters["q"]?.trim() ?: ""
                val category = call.request.queryParameters["category"]?.trim() ?: ""
                val mode = call.request.queryParameters["mode"] ?: "hybrid"
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10

                if (q.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "query parameter 'q' is required"))
                    return@get
                }

                val start = System.currentTimeMillis()
                val results = when {
                    mode == "text" -> searcher.textSearch(q, category, limit)
                    mode == "vector" && category.isNotEmpty() -> searcher.filteredVectorSearch(q, category, limit)
                    mode == "vector" -> searcher.vectorSearch(q, limit)
                    mode == "hybrid" && category.isNotEmpty() -> searcher.filteredHybridSearch(q, category, limit)
                    else -> searcher.hybridSearch(q, limit)
                }

                call.respond(
                    SearchResponse(
                        results = results,
                        totalHits = results.size,
                        mode = mode,
                        durationMs = System.currentTimeMillis() - start,
                    )
                )
            }

            get("/api/categories") {
                call.respond(state.categories)
            }
        }
    }.start(wait = false) // start immediately so health checks pass during indexing

    println("Server listening on port $port")
}
