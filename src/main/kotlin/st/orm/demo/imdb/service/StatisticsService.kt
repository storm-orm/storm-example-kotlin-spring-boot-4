package st.orm.demo.imdb.service

import kotlinx.serialization.Serializable
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import st.orm.demo.imdb.repository.DecadeMovieCount
import st.orm.demo.imdb.repository.GenreRatingStatistics
import st.orm.demo.imdb.repository.MovieGenreRepository
import st.orm.demo.imdb.repository.MovieRepository
import st.orm.demo.imdb.repository.PrincipalRepository
import st.orm.demo.imdb.repository.ProlificActor
import st.orm.template.transactionBlocking

/**
 * Everything the statistics page shows. Serializable, including the Storm
 * entities inside the result types, so the whole view can round-trip
 * through the serialized cache.
 */
@Serializable
data class StatisticsView(
    val decades: List<DecadeMovieCount>,
    val maxDecadeCount: Long,
    val genreStatistics: List<GenreRatingStatistics>,
    val prolificActors: List<ProlificActor>
)

@Service
class StatisticsService(
    private val movieRepository: MovieRepository,
    private val movieGenreRepository: MovieGenreRepository,
    private val principalRepository: PrincipalRepository
) {

    /**
     * All aggregate sections, cached with Spring's cache abstraction. The
     * backing cache stores the view as serialized JSON (see
     * CacheConfiguration) — safe because Storm entities are immutable.
     *
     * Note: @Cacheable does not support suspend functions without the
     * Reactor bridge, so this method is deliberately non-suspend and opens
     * its transaction with transactionBlocking.
     */
    @Cacheable(STATISTICS_CACHE)
    fun buildStatisticsView(): StatisticsView = transactionBlocking(readOnly = true) {
        val decades = movieRepository.countMoviesPerDecade()
        StatisticsView(
            decades = decades,
            maxDecadeCount = decades.maxOfOrNull { it.movieCount } ?: 1L,
            genreStatistics = movieGenreRepository.findGenreRatingStatistics(
                GENRE_MINIMUM_MOVIE_COUNT, GENRE_LIMIT),
            prolificActors = principalRepository.findMostProlificActors(ACTOR_LIMIT)
        )
    }

    companion object {
        const val STATISTICS_CACHE = "statistics"
        private const val GENRE_MINIMUM_MOVIE_COUNT = 50
        private const val GENRE_LIMIT = 10
        private const val ACTOR_LIMIT = 10
    }
}
