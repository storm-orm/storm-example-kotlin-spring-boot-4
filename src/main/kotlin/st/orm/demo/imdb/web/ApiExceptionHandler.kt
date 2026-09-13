package st.orm.demo.imdb.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import st.orm.InvalidCursorException

/**
 * Storm exceptions map to HTTP status codes centrally. A scroll cursor is
 * opaque and is issued for one ordering: a cursor that is malformed, from an
 * earlier format, or issued for another ordering cannot resume the scroll.
 * The client asked with a parameter it cannot fix by retrying, so the answer
 * is a 400 and the list starts over without it.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(InvalidCursorException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidCursor(): String = "Invalid cursor"
}
