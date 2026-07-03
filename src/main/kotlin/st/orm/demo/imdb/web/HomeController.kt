package st.orm.demo.imdb.web

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import st.orm.demo.imdb.service.HomeService

@Controller
class HomeController(private val homeService: HomeService) {

    @GetMapping("/")
    fun home(model: Model): String = runBlocking {
        model.addAttribute("view", homeService.buildHomeView())
        "home"
    }
}
