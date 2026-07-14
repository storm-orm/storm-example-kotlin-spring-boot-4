package st.orm.demo.imdb.repository

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.transaction.TestTransaction
import st.orm.demo.imdb.model.Watchlist
import st.orm.spring.boot.test.DataStormTest
import java.time.Instant

/**
 * The @DataStormTest slice: the counterpart of @DataJpaTest for Storm. Unlike the @StormTest-based
 * tests in this package, which test data logic without a Spring context, the slice sees what production
 * code sees — repositories as Spring beans, the Spring-managed transaction, and SQL failures translated
 * to Spring's DataAccessException hierarchy. The properties attribute points the slice at an in-memory
 * database. Flyway creates the schema exactly as in production, the fixture loads once through @Sql,
 * and each test's own writes roll back afterwards.
 */
@TestMethodOrder(OrderAnnotation::class)
// The slice replaces the PostgreSQL DataSource with an embedded database on Boot 3 and 4 alike.
// Flyway (included in the slice) creates the schema, as in production; the schema.sql used by the
// @StormTest classes stays out of the way.
@DataStormTest(
    properties = [
        "spring.sql.init.mode=never",
        // The application excludes the Spring transaction bridge because it uses the coroutine-native
        // suspend transaction { } blocks. The slice re-enables the bridge: repository calls in tests are
        // blocking, and joining the Spring-managed test transaction is what makes the rollback work.
        "spring.autoconfigure.exclude=",
    ],
)
// The fixture loads once per context (committed): genre ids are identity-generated, and identity
// sequences do not roll back, so a per-method fixture would drift away from its foreign keys.
@Sql(scripts = ["/data.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class WatchlistSliceTest(
    @Autowired private val movieRepository: MovieRepository,
    @Autowired private val watchlistRepository: WatchlistRepository,
) {

    @Test
    @Order(1)
    fun `repositories are injected as Spring beans`() {
        assertTrue(movieRepository.count() > 0)
        assertEquals(0, watchlistRepository.count())
    }

    @Test
    @Order(2)
    fun `every test runs in a rollback transaction`() {
        assertTrue(TestTransaction.isActive())
        val godfather = movieRepository.getById("tt0068646")
        watchlistRepository.insert(Watchlist(movie = godfather, addedAt = Instant.now()))
        assertEquals(1, watchlistRepository.count())
    }

    @Test
    @Order(3)
    fun `the previous test's insert was rolled back`() {
        assertEquals(0, watchlistRepository.count())
    }

    @Test
    @Order(4)
    fun `sql failures arrive as Spring exceptions`() {
        val godfather = movieRepository.getById("tt0068646")
        watchlistRepository.insert(Watchlist(movie = godfather, addedAt = Instant.now()))
        // The duplicate insert violates the primary key; with the starter's exception translation the
        // failure surfaces as Spring's DuplicateKeyException rather than Storm's PersistenceException.
        assertThrows<DuplicateKeyException> {
            watchlistRepository.insert(Watchlist(movie = godfather, addedAt = Instant.now()))
        }
    }
}
