package st.orm.demo.imdb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class ImdbDemoApplication

fun main(args: Array<String>) {
    runApplication<ImdbDemoApplication>(*args)
}
