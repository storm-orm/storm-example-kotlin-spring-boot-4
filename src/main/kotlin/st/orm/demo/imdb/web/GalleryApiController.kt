package st.orm.demo.imdb.web

import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import st.orm.demo.imdb.model.Photo
import st.orm.demo.imdb.service.PersonGalleryService

/**
 * Photo galleries for the person detail pages. The page fetches the gallery
 * asynchronously and renders fine without it.
 */
@RestController
class GalleryApiController(private val personGalleryService: PersonGalleryService) {

    @GetMapping("/api/gallery/person/{personId}")
    fun personGallery(@PathVariable personId: String): ResponseEntity<List<Photo>> = runBlocking {
        personGalleryService.findGallery(personId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
