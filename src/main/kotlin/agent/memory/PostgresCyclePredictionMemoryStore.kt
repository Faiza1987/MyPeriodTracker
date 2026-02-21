package org.tracker.agent.memory

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.tracker.agent.CyclePredictionMemory
import org.tracker.agent.CyclePredictionMemoryStore
import org.tracker.agent.CyclePredictionResult
import org.tracker.domain.Cycle
import org.tracker.domain.CyclePrediction
import org.tracker.domain.PredictionConfidence
import org.tracker.domain.PredictionExplanation
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class PostgresCyclePredictionMemoryStore(
    private val jdbcTemplate: JdbcTemplate
) : CyclePredictionMemoryStore {

    // --- Row Mappers ---

    private val cycleRowMapper = RowMapper { rs: ResultSet, _: Int ->
        Cycle(
            startDate = rs.getObject("period_start", LocalDate::class.java),
            stressLevel = rs.getObject("stress_level") as? Int,
            isIll = rs.getBoolean("illness"),
            flowIntensity = rs.getString("flow_intensity"),
            cervicalMucus = rs.getString("cervical_mucus"),
            notes = rs.getString("notes"),
        )
    }

    private val predictionResultRowMapper = RowMapper { rs: ResultSet, _: Int ->
        CyclePredictionResult(
            predictedDate = rs.getObject("predicted_date", LocalDate::class.java),
            actualDate = rs.getObject("actual_date", LocalDate::class.java),
            differenceInDays = rs.getInt("difference_in_days"),
        )
    }

    // --- Public API ---

    override fun load(userId: UUID): CyclePredictionMemory {
        val cycles = loadCycles(userId)
        val predictionResults = loadPredictionResults(userId)
        val lastPrediction = loadLastPrediction(userId)

        return CyclePredictionMemory(
            cycles = cycles,
            predictionResults = predictionResults,
            lastPrediction = lastPrediction,
        )
    }

    override fun save(userId: UUID, memory: CyclePredictionMemory) {
        saveCycles(userId, memory.cycles)
        savePredictionResults(userId, memory.predictionResults)
        saveLastPrediction(userId, memory.lastPrediction)
    }

    // --- Load helpers ---

    private fun loadCycles(userId: UUID): List<Cycle> =
        jdbcTemplate.query(
            """
            SELECT period_start, stress_level, illness, flow_intensity, cervical_mucus, notes
            FROM cycles
            WHERE user_id = ?
            ORDER BY period_start
            """.trimIndent(),
            cycleRowMapper,
            userId
        )

    private fun loadPredictionResults(userId: UUID): List<CyclePredictionResult> =
        jdbcTemplate.query(
            """
            SELECT predicted_date, actual_date, difference_in_days
            FROM prediction_results
            WHERE user_id = ?
            ORDER BY created_at
            """.trimIndent(),
            predictionResultRowMapper,
            userId
        )

    private fun loadLastPrediction(userId: UUID): CyclePrediction? {
        val results = jdbcTemplate.query(
            """
            SELECT last_predicted_date, last_confidence, last_reasons
            FROM user_state
            WHERE user_id = ?
            """.trimIndent(),
            { rs: ResultSet, _: Int ->
                val date = rs.getObject("last_predicted_date", LocalDate::class.java)
                val confidence = rs.getString("last_confidence")
                val reasons = (rs.getArray("last_reasons")?.array as? Array<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                if (date != null && confidence != null) {
                    CyclePrediction(
                        predictedStartDate = date,
                        explanation = PredictionExplanation(
                            confidence = PredictionConfidence.valueOf(confidence),
                            reasons = reasons
                        )
                    )
                } else null
            },
            userId
        )

        return results.firstOrNull()
    }

    // --- Save helpers ---

    private fun saveCycles(userId: UUID, cycles: List<Cycle>) {
        val existingDates = jdbcTemplate.queryForList(
            "SELECT period_start FROM cycles WHERE user_id = ?",
            LocalDate::class.java,
            userId
        ).toSet()

        cycles
            .filterNot { it.startDate in existingDates }
            .forEach { cycle ->
                jdbcTemplate.update(
                    """
                    INSERT INTO cycles 
                        (user_id, period_start, period_duration, stress_level, illness, flow_intensity, cervical_mucus, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    userId,
                    cycle.startDate,
                    7, // default period duration
                    cycle.stressLevel,
                    cycle.isIll,
                    cycle.flowIntensity,
                    cycle.cervicalMucus,
                    cycle.notes,
                )
            }
    }

    private fun savePredictionResults(userId: UUID, results: List<CyclePredictionResult>) {
        // Avoid re-inserting results already in the DB by checking (predicted_date, actual_date)
        val existingPairs = jdbcTemplate.query(
            "SELECT predicted_date, actual_date FROM prediction_results WHERE user_id = ?",
            { rs: ResultSet, _: Int ->
                rs.getObject("predicted_date", LocalDate::class.java) to
                        rs.getObject("actual_date", LocalDate::class.java)
            },
            userId
        ).toSet()

        results
            .filterNot { (it.predictedDate to it.actualDate) in existingPairs }
            .forEach { result ->
                jdbcTemplate.update(
                    """
                    INSERT INTO prediction_results (user_id, predicted_date, actual_date, difference_in_days)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                    userId,
                    result.predictedDate,
                    result.actualDate,
                    result.differenceInDays,
                )
            }
    }

    private fun saveLastPrediction(userId: UUID, prediction: CyclePrediction?) {
        if (prediction == null) return

        val reasonsArray = jdbcTemplate.dataSource!!
            .connection
            .use { conn -> conn.createArrayOf("text", prediction.explanation.reasons.toTypedArray()) }

        jdbcTemplate.update(
            """
            INSERT INTO user_state (user_id, last_predicted_date, last_confidence, last_reasons, updated_at)
            VALUES (?, ?, ?, ?, now())
            ON CONFLICT (user_id) DO UPDATE SET
                last_predicted_date = EXCLUDED.last_predicted_date,
                last_confidence     = EXCLUDED.last_confidence,
                last_reasons        = EXCLUDED.last_reasons,
                updated_at          = EXCLUDED.updated_at
            """.trimIndent(),
            userId,
            prediction.predictedStartDate,
            prediction.explanation.confidence.name,
            reasonsArray,
        )
    }
}