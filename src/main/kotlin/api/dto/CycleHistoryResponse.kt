package org.tracker.api.dto

import org.tracker.domain.Cycle
import org.tracker.domain.CycleHistory

data class CycleHistoryResponse(
    val totalCycles: Int,
    val cycles: List<CycleEntry>,
    val stats: CycleStatsResponse?
) {
    data class CycleEntry(
        val startDate: String,
        val stressLevel: Int?,
        val isIll: Boolean,
        val flowIntensity: String?,
        val cervicalMucus: String?,
        val notes: String?
    )

    data class CycleStatsResponse(
        val averageCycleLength: Int,
        val shortestCycle: Int,
        val longestCycle: Int,
        val standardDeviation: Double
    )

    companion object {
        fun from(cycles: List<Cycle>): CycleHistoryResponse {
            val history = CycleHistory(cycles)
            val stats = if (history.canPredict()) {
                val s = history.stats()
                CycleStatsResponse(
                    averageCycleLength = s.average,
                    shortestCycle = s.shortest,
                    longestCycle = s.longest,
                    standardDeviation = s.standardDeviation
                )
            } else null

            return CycleHistoryResponse(
                totalCycles = cycles.size,
                cycles = cycles
                    .sortedByDescending { it.startDate }
                    .map { cycle ->
                        CycleEntry(
                            startDate = cycle.startDate.toString(),
                            stressLevel = cycle.stressLevel,
                            isIll = cycle.isIll,
                            flowIntensity = cycle.flowIntensity,
                            cervicalMucus = cycle.cervicalMucus,
                            notes = cycle.notes
                        )
                    },
                stats = stats
            )
        }
    }
}