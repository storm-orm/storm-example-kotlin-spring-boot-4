package st.orm.demo.imdb.web

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import st.orm.demo.imdb.service.MovieService
import st.orm.demo.imdb.service.WatchlistService

@Controller
class MovieController(private val movieService: MovieService) {

    @GetMapping("/movie/{movieId}")
    fun movieDetail(@PathVariable movieId: String, model: Model): String = runBlocking {
        val detail = movieService.viewMovie(movieId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown movie: $movieId")
        model.addAttribute("detail", detail)
        "movie"
    }
}

@RestController
class WatchlistApiController(private val watchlistService: WatchlistService) {

    @PostMapping("/api/watchlist/{movieId}")
    fun toggle(@PathVariable movieId: String): WatchlistState = runBlocking {
        WatchlistState(onWatchlist = watchlistService.toggle(movieId))
    }
}
