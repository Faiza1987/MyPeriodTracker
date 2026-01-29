package org.tracker.domain

import java.time.LocalDate

data class CycleSummary(
    val cycleDay: Int,
    val fertileWindow: Pair<LocalDate, LocalDate>,
    val pregnancyRiskAssessment: PregnancyRiskAssessment
)