package org.tracker.agent.memory


import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
import kotlin.test.assertTrue
import org.testcontainers.utility.DockerImageName

@Testcontainers
class PostgresCyclePredictionMemoryStoreTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            // Force the environment variable inside the JVM process
            withEnv("DOCKER_HOST", "unix:///Users/faizaahsan/.docker/run/docker.sock")
        }
    }


    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var store: PostgresCyclePredictionMemoryStore

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val dataSource = org.springframework.jdbc.datasource.DriverManagerDataSource(
            postgres.jdbcUrl,
            postgres.username,
            postgres.password
        )

        jdbcTemplate = JdbcTemplate(dataSource)
        store = PostgresCyclePredictionMemoryStore(jdbcTemplate)

        createSchema()

        jdbcTemplate.update(
            "INSERT INTO users (id) VALUES (?)",
            userId
        )
    }

    @Test
    fun `load returns empty memory when user has no cycles`() {
        val memory = store.load(userId)

        assertTrue(memory.cycles.isEmpty())
    }


    private fun createSchema() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY
            )
            """.trimIndent()
        )

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS cycles (
                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                period_start    DATE NOT NULL,
                period_duration INT NOT NULL,
                stress_level    INT,
                illness         BOOLEAN DEFAULT FALSE,
                flow_intensity  VARCHAR(20),
                cervical_mucus  VARCHAR(20),
                notes           TEXT
            )
            """.trimIndent()
        )

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS user_state (
                user_id             UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                last_predicted_date DATE,
                last_confidence     VARCHAR(20),
                last_reasons        TEXT[],
                updated_at          TIMESTAMP NOT NULL DEFAULT now()
            )
            """.trimIndent()
        )

        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS prediction_results (
                id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                predicted_date      DATE NOT NULL,
                actual_date         DATE NOT NULL,
                difference_in_days  INT NOT NULL,
                created_at          TIMESTAMP NOT NULL DEFAULT now()
            )
            """.trimIndent()
        )
    }


}
