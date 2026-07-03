package st.orm.demo.imdb

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import st.orm.demo.imdb.serialization.KotlinxSerializedCache
import st.orm.demo.imdb.service.StatisticsService
import st.orm.demo.imdb.service.StatisticsView

/**
 * Spring cache backed by serialized JSON values. An in-memory cache would
 * work with plain object references, but serializing every value — like an
 * external store such as Redis would — demonstrates that Storm's immutable
 * entities survive the round-trip unchanged.
 */
@Configuration
@EnableCaching
class CacheConfiguration {

    @Bean
    fun cacheManager(): CacheManager = SimpleCacheManager().apply {
        setCaches(
            listOf(
                KotlinxSerializedCache(StatisticsService.STATISTICS_CACHE, StatisticsView.serializer())
            )
        )
    }
}
