package st.orm.demo.imdb.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * The statistics page aggregates every movie, rating and credit, which takes a
 * moment, and its cache lives in memory, so it starts empty after each restart.
 * Filling it in the background once the application is ready, after the import
 * (an ApplicationRunner) has put the data in place, spares the first visitor
 * that wait. The call goes through the StatisticsService bean, so @Cacheable
 * stores the result. A failure is logged, and the page computes the view itself.
 */
@Component
class StatisticsWarmUp(private val statisticsService: StatisticsService) {

    private val logger = LoggerFactory.getLogger(StatisticsWarmUp::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun warmUp() {
        Thread.ofVirtual().name("statistics-warm-up").start {
            runCatching { statisticsService.buildStatisticsView() }
                .onFailure { logger.warn("Could not prepare the statistics in advance.", it) }
        }
    }
}
