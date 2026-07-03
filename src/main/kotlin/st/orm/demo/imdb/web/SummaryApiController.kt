package st.orm.demo.imdb.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import st.orm.demo.imdb.service.ExternalSummary
import st.orm.demo.imdb.service.WikipediaSummaryService

/**
 * Wikipedia enrichment for the detail pages. The pages fetch these
 * summaries asynchronously and render fine without them.
 */
@RestController
class SummaryApiController(private val wikipediaSummaryService: WikipediaSummaryService) {

    @GetMapping("/api/summary/movie/{movieId}")
    fun movieSummary(@PathVariable movieId: String): ResponseEntity<ExternalSummary> =
        wikipediaSummaryService.findMovieSummary(movieId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @GetMapping("/api/summary/person/{personId}")
    fun personSummary(@PathVariable personId: String): ResponseEntity<ExternalSummary> =
        wikipediaSummaryService.findPersonSummary(personId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
}
