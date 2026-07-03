package st.orm.demo.imdb.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import st.orm.demo.imdb.service.WatchlistService

@Controller
class WatchlistController(private val watchlistService: WatchlistService) {

    @GetMapping("/watchlist")
    fun watchlist(@RequestParam(defaultValue = "1") page: Int, model: Model): String {
        // Page numbers in the URL are 1-based; Storm pages are 0-based.
        val watchlistPage = watchlistService.findPage((page - 1).coerceAtLeast(0))
        model.addAttribute("watchlistPage", watchlistPage)
        model.addAttribute("currentPage", page)
        return "watchlist"
    }
}
