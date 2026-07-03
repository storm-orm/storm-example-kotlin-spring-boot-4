package st.orm.demo.imdb.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import st.orm.demo.imdb.service.StatisticsService

@Controller
class StatisticsController(private val statisticsService: StatisticsService) {

    @GetMapping("/statistics")
    fun statistics(model: Model): String {
        val view = statisticsService.buildStatisticsView()
        model.addAttribute("decades", view.decades)
        model.addAttribute("maxDecadeCount", view.maxDecadeCount)
        model.addAttribute("genreStatistics", view.genreStatistics)
        model.addAttribute("prolificActors", view.prolificActors)
        return "statistics"
    }
}
