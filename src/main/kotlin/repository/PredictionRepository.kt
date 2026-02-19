package org.tracker.repository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.tracker.domain.CyclePrediction
import java.util.UUID

@Repository
class PredictionRepository(private val jdbcClient: JdbcClient) {

    fun savePrediction(userId: UUID, prediction: CyclePrediction) {
        jdbcClient.sql("""
            INSERT INTO cycle_predictions (user_id, predicted_start_date, confidence, reasons)
            VALUES (:userId, :startDate, :confidence, :reasons)
        """.trimIndent())
            .param("userId", userId)
            .param("startDate", prediction.predictedStartDate)
            .param("confidence", prediction.explanation.confidence.name)
            // Postgres handles the transition from List<String> to TEXT[] via toTypedArray()
            .param("reasons", prediction.explanation.reasons.toTypedArray())
            .update()

    }
}