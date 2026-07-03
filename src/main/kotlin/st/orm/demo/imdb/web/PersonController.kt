package st.orm.demo.imdb.web

import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException
import st.orm.demo.imdb.service.PersonService

@Controller
class PersonController(private val personService: PersonService) {

    @GetMapping("/person/{personId}")
    fun personDetail(@PathVariable personId: String, model: Model): String = runBlocking {
        val detail = personService.findPersonDetail(personId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown person: $personId")
        model.addAttribute("person", detail.person)
        model.addAttribute("filmography", detail.filmography)
        model.addAttribute("statistics", detail.statistics)
        "person"
    }
}
