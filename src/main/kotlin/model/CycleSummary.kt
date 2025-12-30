package org.tracker.model

import org.tracker.models.PregnancyRisk
import java.time.LocalDate

data class CycleSummary(
    val cycleDay: Int,
    val fertileWindow: Pair<LocalDate, LocalDate>,
    val pregnancyRisk: PregnancyRisk
)
