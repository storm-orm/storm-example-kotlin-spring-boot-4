import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("com.google.devtools.ksp") version "2.2.21-2.0.5"
    id("org.springframework.boot") version "4.1.0"
    // The Storm plugin imports the BOM, adds storm-kotlin and storm-core, wires the
    // metamodel processor to KSP, and selects the compiler-plugin variant matching Kotlin.
    id("st.orm") version "1.13.0"
}

group = "st.orm.demo"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    // mavenLocal first so locally built Storm versions (not yet on Central)
    // resolve during development against the framework.
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Tracer for the trace-context SQL comments; spans stay local without an exporter.
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("st.orm:storm-kotlin-spring-boot-starter")
    implementation("st.orm:storm-jackson3")
    implementation("st.orm:storm-kotlinx-serialization")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    runtimeOnly("st.orm:storm-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("st.orm:storm-test")
    testImplementation("st.orm:storm-spring-boot-test-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("st.orm:storm-h2")
    testRuntimeOnly("com.h2database:h2:2.3.232")
    testImplementation("com.microsoft.playwright:playwright:1.61.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

// Playwright interface tests run against the live application: start the
// app (./gradlew bootRun), then run ./gradlew e2eTest.
tasks.register<Test>("e2eTest") {
    description = "Runs Playwright interface tests against the running application."
    group = "verification"
    useJUnitPlatform {
        includeTags("e2e")
    }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    systemProperty("app.baseUrl", System.getProperty("app.baseUrl") ?: "http://localhost:8080")
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("installPlaywrightBrowsers") {
    description = "Downloads the Chromium browser used by the Playwright tests."
    group = "verification"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "com.microsoft.playwright.CLI"
    args("install", "chromium")
}
