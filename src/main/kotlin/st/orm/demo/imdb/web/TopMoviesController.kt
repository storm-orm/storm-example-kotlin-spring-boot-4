package st.orm.demo.imdb.web

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import st.orm.demo.imdb.repository.TopMoviesSort
import st.orm.demo.imdb.service.TopMoviesService

@Controller
class TopMoviesController(private val topMoviesService: TopMoviesService) {

    @GetMapping("/top")
    fun topMovies(
        @RequestParam(required = false) genre: String?,
        @RequestParam(defaultValue = "rating") sort: String,
        model: Model
    ): String = runBlocking {
        val sortBy = if (sort == "year") TopMoviesSort.YEAR else TopMoviesSort.RATING
        val view = topMoviesService.findTopMovies(genre, sortBy)
        model.addAttribute("genres", view.genres)
        model.addAttribute("selectedGenre", view.selectedGenre)
        model.addAttribute("sort", if (sortBy == TopMoviesSort.YEAR) "year" else "rating")
        model.addAttribute("entries", view.entries)
        "top"
    }
}
