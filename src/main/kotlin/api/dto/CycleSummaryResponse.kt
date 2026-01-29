package org.tracker.api.dto

import org.tracker.domain.CycleSummary

data class CycleSummaryResponse(
    val cycleDay: Int,
    val fertileWindowStart: String,
    val fertileWindowEnd: String,
    val pregnancyRisk: String,
    val reasons: List<String>,
) {

    companion object {
        fun from(summary: CycleSummary): CycleSummaryResponse =
            CycleSummaryResponse(
                cycleDay = summary.cycleDay,
                fertileWindowStart = summary.fertileWindow.first.toString(),
                fertileWindowEnd = summary.fertileWindow.second.toString(),
                pregnancyRisk = summary.pregnancyRiskAssessment.risk.name,
                reasons = summary.pregnancyRiskAssessment.reasons
            )
    }
}
