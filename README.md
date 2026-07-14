# Storm Movies · Kotlin + Spring Boot 4 example

An example movie browser built with [Storm ORM](https://orm.st) on Spring Boot 4
and Kotlin. It imports the public [IMDB dataset](https://datasets.imdbws.com/)
into PostgreSQL and serves a server-rendered web app (Thymeleaf + a little
vanilla JS) for browsing movies, people, genres, ratings, and a watchlist.

The project exists to show what idiomatic Storm looks like in a real Spring
Boot application: immutable data-class entities, metamodel-based queries,
coroutine-native transactions, and schema validation. No JPA, no proxies, no
persistence context.

## Stack

- Kotlin 2.2 / Java 21, Spring Boot 4.1 (WebMVC + Thymeleaf, virtual threads enabled)
- Storm ORM (`storm-kotlin-spring-boot-starter`) with the KSP metamodel
  generator and the Storm compiler plugin
- PostgreSQL 17 (Docker Compose) with Flyway migrations
- kotlinx.serialization for JSON APIs and cache values; Jackson for parsing
  external APIs
- JUnit 5 + `storm-test` on H2 for repository tests, Playwright for interface tests

## Running the application

Prerequisites: JDK 21 and Docker.

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Start the application
./gradlew bootRun

# 3. Open the app
open http://localhost:8080
```

On first startup the app runs the Flyway migration and imports the IMDB
dataset: movies with at least 1,000 votes (configurable via
`imdb.import.minimum-vote-count`), plus their genres, cast, crew, and ratings.
The dataset files (~1.2 GB) are downloaded once and cached in `./data`, then
streamed through Storm's suspending batch inserts, so expect the first
startup to take a few minutes. The import is skipped entirely on subsequent startups
once movie data is present.

To start over with an empty database:

```bash
docker compose down -v
```

Movie posters, person photos, and plot summaries are fetched at runtime from
the IMDB suggestion API and the Wikipedia REST API, so the app looks best with
internet access.

## Project layout

```
src/main/kotlin/st/orm/demo/imdb/
├── model/          Storm entities (@PK, @FK) and projections
├── repository/     EntityRepository interfaces with QueryBuilder queries
├── service/        Business logic in suspend `transaction { }` blocks,
│                   plus the streaming IMDB importer
├── web/            MVC controllers (pages) and REST controllers (/api/**)
└── serialization/  kotlinx.serialization support: custom serializers and
                    the JSON-serialized Spring cache
src/main/resources/
├── db/migration/   Flyway schema (V1__create_schema.sql)
├── templates/      Thymeleaf views
└── static/         CSS, JS, images
```

## What to look at

Each part of the app demonstrates a Storm feature:

- **Entities** (`model/`): immutable data classes with `@PK`, `@FK`, `@UK`,
  and composite keys (`MovieGenre`, `Principal`). `MovieView` is a
  database-view-backed projection; `MovieSummary` / `PersonSummary` select a
  subset of columns.
- **Repositories** (`repository/`): `EntityRepository` interfaces with default
  methods using the type-safe QueryBuilder and generated metamodel
  (`Movie_.startYear`, `Principal_.person`). Aggregations return plain data
  classes; computed expressions use SQL template lambdas with metamodel
  references.
- **Transactions** (`service/`): Storm's coroutine-native suspend
  `transaction { }` blocks at the service level, bridged with `runBlocking`
  only at MVC entry points. Spring-managed Storm transactions are deliberately
  excluded in `application.yaml` because suspend mode manages transactions on
  the `DataSource` directly. `StatisticsService` is the one declarative
  `@Transactional` example (`@Cacheable` and `@Transactional` do not support
  suspend functions); the Storm repository calls inside it join the
  Spring-managed transaction.
- **Observability** (`application.yaml`): every query and transaction is
  reported as a Micrometer Observation (`storm.query`, `storm.transaction`),
  surfaced by Actuator at `/actuator/metrics/storm.query`. Query observations
  follow the OpenTelemetry database semantic conventions, and the trace
  context rides along as a SQL comment on every statement.
- **Streaming import** (`service/ImdbDataImporter.kt`): Flow-based pipeline
  that parses TSV rows into entities and hands them to Storm's suspending
  batch insert, one pass per file, without materializing entity lists.
- **Schema validation**: on by default. The starter verifies every entity
  against the live database schema at startup;
  `EntitySchemaValidationTest` does the same in the test suite.
- **Serialization** (`serialization/`, `web/ApiModels.kt`): Storm entities
  serialized with kotlinx.serialization for the REST endpoints, and a Spring
  cache that stores values as serialized JSON to prove entities survive the
  round-trip (`CacheConfiguration.kt`).

## Testing

```bash
./gradlew test
```

Repository tests run on an in-memory H2 database via `@StormTest`, so no
Docker is required. Tests receive an `ORMTemplate` and a `SqlCapture` as parameters, so
they can assert on the SQL Storm generates.

The Playwright interface tests run against a live application:

```bash
./gradlew installPlaywrightBrowsers   # once
./gradlew bootRun                     # in one terminal
./gradlew e2eTest                     # in another
```

## Configuration

Everything lives in `src/main/resources/application.yaml`. The defaults match
the Compose file (database `imdb`, user/password `storm` on `localhost:5432`).
Import behavior is tunable under `imdb.import` (cache directory, minimum vote
count, dataset base URL).
