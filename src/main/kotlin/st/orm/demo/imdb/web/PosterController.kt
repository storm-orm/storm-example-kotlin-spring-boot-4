package st.orm.demo.imdb.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves movie posters and person photos through IMDB's public suggestion
 * API and redirects to the CDN image — the API accepts both tconst (tt...)
 * and nconst (nm...) identifiers. Serving images through this endpoint
 * avoids CORS and WAF issues that direct client-side calls to IMDB would
 * hit. Results — including "no image" — are cached for the lifetime of the
 * application.
 */
@RestController
class PosterController(private val objectMapper: ObjectMapper) {

    private val imageUrlsByImdbId = ConcurrentHashMap<String, Optional<String>>()

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    @GetMapping("/api/poster/{imdbId}", "/api/photo/{imdbId}")
    fun image(@PathVariable imdbId: String): ResponseEntity<Void> {
        val imageUrl = imageUrlsByImdbId.computeIfAbsent(imdbId) { id ->
            Optional.ofNullable(fetchImageUrl(id))
        }
        return imageUrl
            .map { url -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build<Void>() }
            .orElseGet { ResponseEntity.notFound().build() }
    }

    /**
     * The suggestion API's video entries carry landscape trailer stills —
     * the closest thing the public API has to a Netflix-style backdrop.
     */
    @GetMapping("/api/backdrop/{imdbId}")
    fun backdrop(@PathVariable imdbId: String): ResponseEntity<Void> {
        val imageUrl = imageUrlsByImdbId.computeIfAbsent("backdrop:$imdbId") {
            Optional.ofNullable(fetchBackdropUrl(imdbId))
        }
        return imageUrl
            .map { url -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build<Void>() }
            .orElseGet { ResponseEntity.notFound().build() }
    }

    private fun fetchBackdropUrl(imdbId: String): String? = try {
        val uri = URI.create("https://v3.sg.media-imdb.com/suggestion/x/$imdbId.json?includeVideos=1")
        val response = httpClient.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) null else {
            val suggestions = objectMapper.readTree(response.body()).path("d")
            suggestions.firstOrNull { it.path("id").asString("") == imdbId }
                ?.path("v")
                ?.maxByOrNull { video -> video.path("i").path("width").asInt(0) }
                ?.path("i")?.path("imageUrl")?.asString(null)
                ?.replace("._V1_.", "._V1_SX1280.")
        }
    } catch (ignored: Exception) {
        null
    }

    private fun fetchImageUrl(imdbId: String): String? = try {
        val uri = URI.create("https://v3.sg.media-imdb.com/suggestion/x/$imdbId.json")
        val response = httpClient.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) null else {
            val suggestions = objectMapper.readTree(response.body()).path("d")
            suggestions.firstOrNull { it.path("id").asString("") == imdbId }
                ?.path("i")?.path("imageUrl")?.asString(null)
                ?.replace("._V1_.", "._V1_SX400.")
        }
    } catch (ignored: Exception) {
        null
    }
}
