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
        );
        """.trimIndent()
        )

        jdbcTemplate.execute(
            """
        CREATE TABLE IF NOT EXISTS cycles (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            period_start DATE NOT NULL,
            period_duration INT NOT NULL
        );
        """.trimIndent()
        )
    }


}
