package st.orm.demo.imdb.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.springframework.cache.concurrent.ConcurrentMapCache
import st.orm.serialization.StormSerializers

/**
 * A Spring Cache that stores its values as serialized JSON — the way an
 * external cache like Redis would. Every hit decodes the payload back into
 * objects, so each read exercises the full serialization round-trip.
 *
 * This works because Storm entities are immutable data classes: no proxies,
 * no session state, no lazy-loading surprises — what serializes is exactly
 * what was queried, and the decoded copy compares equal to it. The Json
 * instance carries StormSerializers so entity graphs containing Ref fields
 * serialize correctly as well.
 */
class KotlinxSerializedCache<T : Any>(
    name: String,
    private val serializer: KSerializer<T>,
    private val json: Json = Json { serializersModule = StormSerializers }
) : ConcurrentMapCache(name) {

    @Suppress("UNCHECKED_CAST")
    override fun toStoreValue(userValue: Any?): Any =
        json.encodeToString(serializer, userValue as T)

    override fun fromStoreValue(storeValue: Any?): Any =
        json.decodeFromString(serializer, storeValue as String)
}
