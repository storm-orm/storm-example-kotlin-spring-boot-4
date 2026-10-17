package st.orm.demo.imdb.web

import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * What a lookup of an image found: its URL, no image at all, or nothing yet
 * because IMDB could not be asked or did not answer. Only the first two are
 * answers worth keeping.
 */
sealed interface ImageLookup {
    data class Found(val url: String) : ImageLookup
    data object None : ImageLookup
    data object Failed : ImageLookup
}

/**
 * Resolves movie posters and person photos through IMDB's public suggestion
 * API and redirects to the CDN image — the API accepts both tconst (tt...)
 * and nconst (nm...) identifiers. Serving images through this endpoint
 * avoids CORS and WAF issues that direct client-side calls to IMDB would
 * hit. An answer — an image, or none — is cached for the lifetime of the
 * application; a failed lookup is not, so the next request asks again
 * rather than showing no image until a restart.
 */
@RestController
class PosterController(private val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(PosterController::class.java)

    private val lookupsByImdbId = ConcurrentHashMap<String, ImageLookup>()

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(3))
        .build()

    @GetMapping("/api/poster/{imdbId}", "/api/photo/{imdbId}")
    fun image(@PathVariable imdbId: String): ResponseEntity<Void> =
        respondImage(lookup(imdbId) { fetch(imdbId, includeVideos = false) { entry ->
            entry.path("i").path("imageUrl").asString(null)?.replace("._V1_.", "._V1_SX400.")
        } })

    /**
     * The suggestion API's video entries carry landscape trailer stills —
     * the closest thing the public API has to a Netflix-style backdrop.
     */
    @GetMapping("/api/backdrop/{imdbId}")
    fun backdrop(@PathVariable imdbId: String): ResponseEntity<Void> =
        respondImage(lookup("backdrop:$imdbId") { fetch(imdbId, includeVideos = true) { entry ->
            entry.path("v")
                .maxByOrNull { video -> video.path("i").path("width").asInt(0) }
                ?.path("i")?.path("imageUrl")?.asString(null)
                ?.replace("._V1_.", "._V1_SX1280.")
        } })

    /** Returns the cached answer for the key, or looks it up, keeping it unless the lookup failed. */
    private fun lookup(key: String, fetch: () -> ImageLookup): ImageLookup =
        lookupsByImdbId[key] ?: fetch().also { if (it != ImageLookup.Failed) lookupsByImdbId[key] = it }

    private fun fetch(imdbId: String, includeVideos: Boolean, image: (JsonNode) -> String?): ImageLookup = try {
        val uri = URI.create("https://v3.sg.media-imdb.com/suggestion/x/$imdbId.json" + if (includeVideos) "?includeVideos=1" else "")
        val request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            logger.warn("Image lookup for {} failed: IMDB answered {}.", imdbId, response.statusCode())
            ImageLookup.Failed
        } else {
            objectMapper.readTree(response.body()).path("d")
                .firstOrNull { it.path("id").asString("") == imdbId }
                ?.let(image)
                ?.let { ImageLookup.Found(it) }
                ?: ImageLookup.None
        }
    } catch (e: Exception) {
        logger.warn("Image lookup for {} failed: {}.", imdbId, e.toString())
        ImageLookup.Failed
    }

    /**
     * Responds with a 302 redirect to the image, or 404 when there is none; both are answers, which
     * the browser caches for a day, as they are cached here: without it the browser asks again for
     * every image each time a page or the search dropdown shows it. A failed lookup answers 503 and
     * is not cached, so the image is asked for again the next time it is shown.
     */
    private fun respondImage(lookup: ImageLookup): ResponseEntity<Void> = when (lookup) {
        is ImageLookup.Found -> ResponseEntity.status(HttpStatus.FOUND)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .location(URI.create(lookup.url))
            .build()
        ImageLookup.None -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
            .build()
        ImageLookup.Failed -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .cacheControl(CacheControl.noStore())
            .build()
    }
}
