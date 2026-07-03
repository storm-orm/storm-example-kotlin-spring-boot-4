package st.orm.demo.imdb.service

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties(prefix = "imdb.import")
data class ImdbImportProperties(
    /** Directory where downloaded IMDB dataset files are cached. */
    val cacheDirectory: Path = Path.of("./data"),
    /** Only movies with at least this many votes are imported. */
    val minimumVoteCount: Int = 1000,
    /** Base URL of the public IMDB dataset files. */
    val datasetBaseUrl: String = "https://datasets.imdbws.com"
)
