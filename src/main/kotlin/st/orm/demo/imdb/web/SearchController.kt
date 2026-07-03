package st.orm.demo.imdb.web

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import st.orm.demo.imdb.service.SearchService

@Controller
class SearchController(private val searchService: SearchService) {

    @GetMapping("/search")
    fun search(@RequestParam(required = false) query: String?, model: Model): String = runBlocking {
        val searchQuery = query?.trim().orEmpty()
        model.addAttribute("query", searchQuery)
        if (searchQuery.isNotEmpty()) {
            val results = searchService.search(searchQuery)
            model.addAttribute("movieWindow", results.movieWindow)
            model.addAttribute("personWindow", results.personWindow)
        }
        "search"
    }
}

@RestController
class SearchApiController(private val searchService: SearchService) {

    @GetMapping("/api/search/suggestions")
    fun suggestions(@RequestParam query: String): SearchSuggestions = runBlocking {
        val suggestions = searchService.findSuggestions(query)
        SearchSuggestions(
            movies = suggestions.movies.map { it.toSearchItem() },
            persons = suggestions.persons.map { it.toSearchItem() }
        )
    }

    @GetMapping("/api/search/movies")
    fun movies(
        @RequestParam query: String,
        @RequestParam(required = false) cursor: String?
    ): SearchWindow<MovieSearchItem> {
        val window = searchService.scrollMovies(query, cursor)
        return SearchWindow(window.content().map { it.toSearchItem() }, window.nextCursor())
    }

    @GetMapping("/api/search/persons")
    fun persons(
        @RequestParam query: String,
        @RequestParam(required = false) cursor: String?
    ): SearchWindow<PersonSearchItem> {
        val window = searchService.scrollPersons(query, cursor)
        return SearchWindow(window.content().map { it.toSearchItem() }, window.nextCursor())
    }
}
