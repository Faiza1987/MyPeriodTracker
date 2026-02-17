package org.tracker.agent.memory

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.tracker.agent.CyclePredictionMemory
import org.tracker.agent.CyclePredictionMemoryStore
import org.tracker.domain.Cycle
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class PostgresCyclePredictionMemoryStore(
    private val jdbcTemplate: JdbcTemplate
) : CyclePredictionMemoryStore {

    private val cycleRowMapper = RowMapper { rs: ResultSet, _: Int ->
        Cycle(
            startDate = rs.getObject("period_start", LocalDate::class.java)
        )
    }

    override fun load(userId: UUID): CyclePredictionMemory {
        val cycles = jdbcTemplate.query(
            """
            SELECT period_start
            FROM cycles
            WHERE user_id = ?
            ORDER BY period_start
            """.trimIndent(),
            cycleRowMapper,
            userId
        )

        return CyclePredictionMemory(
            cycles = cycles
        )
    }

    override fun save(userId: UUID, memory: CyclePredictionMemory) {
        val existingDates = jdbcTemplate.queryForList(
            """
            SELECT period_start
            FROM cycles
            WHERE user_id = ?
            """.trimIndent(),
            LocalDate::class.java,
            userId
        ).toSet()

        val newCycles = memory.cycles
            .map { it.startDate }
            .filterNot { it in existingDates }

        newCycles.forEach { startDate ->
            jdbcTemplate.update(
                """
                INSERT INTO cycles (user_id, period_start, period_duration)
                VALUES (?, ?, ?)
                """.trimIndent(),
                userId,
                startDate,
                7 // default for now, matches schema
            )
        }
    }
}
