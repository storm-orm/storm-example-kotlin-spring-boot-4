package st.orm.demo.imdb.web

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import st.orm.demo.imdb.service.BrowseService

@Controller
class BrowseController(private val browseService: BrowseService) {

    @GetMapping("/browse/{genreName}")
    fun browse(@PathVariable genreName: String, model: Model): String = runBlocking {
        val view = browseService.browseGenre(genreName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown genre: $genreName")
        model.addAttribute("genre", view.genre)
        model.addAttribute("movieCount", view.movieCount)
        model.addAttribute("movieWindow", view.movieWindow)
        "browse"
    }
}

@RestController
class BrowseApiController(private val browseService: BrowseService) {

    @GetMapping("/api/browse/{genreName}")
    fun browseMovies(
        @PathVariable genreName: String,
        @RequestParam(required = false) cursor: String?
    ): SearchWindow<MovieSearchItem> = runBlocking {
        val window = browseService.scrollGenre(genreName, cursor)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown genre: $genreName")
        SearchWindow(window.content().map { it.toSearchItem() }, window.nextCursor())
    }
}
