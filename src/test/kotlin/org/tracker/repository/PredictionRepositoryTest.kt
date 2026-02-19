package org.tracker.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.tracker.domain.*
import java.time.LocalDate
import java.util.*

@SpringBootTest
@Testcontainers
class PredictionRepositoryTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    lateinit var predictionRepository: PredictionRepository

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @Test
    fun `should save and persist cycle prediction`() {
        // Arrange
        val userId = UUID.randomUUID()
        insertTestUser(userId)


        val prediction = CyclePrediction(
            predictedStartDate = LocalDate.of(2026, 3, 10),
            explanation = PredictionExplanation(
                confidence = PredictionConfidence.HIGH,
                reasons = listOf("Stable history", "Low stress logged")
            )
        )

        // Act
        predictionRepository.savePrediction(userId, prediction)

        // Assert
        val savedPrediction = findSavedPrediction(userId)
        val expectedDate = LocalDate.of(2026, 3, 10)

        assertEquals(userId, savedPrediction["user_id"])
        assertEquals(expectedDate, savedPrediction["predicted_start_date"])
        assertEquals("HIGH", savedPrediction["confidence"])

        val reasons = savedPrediction["reasons"] as Array<*>
        assertEquals(2, reasons.size)
        assertEquals("Stable history", reasons[0])

    }

    private fun insertTestUser(userId: UUID) {
        jdbcClient.sql("""
            INSERT INTO users (id, average_cycle_length, typical_period_length) 
            VALUES (?, ?, ?)
        """.trimIndent())
            .params(userId, 28, 5)
            .update()

    }

    private fun findSavedPrediction(userId: UUID): Map<String, Any?> {
        return jdbcClient.sql("SELECT * FROM cycle_predictions WHERE user_id = ?")
            .params(userId)
            .query { rs, _ ->
                mapOf(
                    "user_id" to rs.getObject("user_id", UUID::class.java),
                    "predicted_start_date" to rs.getDate("predicted_start_date").toLocalDate(),
                    "confidence" to rs.getString("confidence"),
                    "reasons" to (rs.getArray("reasons").array as Array<*>)
                )
            }.single()
    }
}