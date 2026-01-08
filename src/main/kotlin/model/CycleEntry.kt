package org.tracker.models

import org.tracker.model.IntercourseEvent
import java.time.LocalDate


data class CycleEntry(
    val periodStart: LocalDate,
    val periodDuration: Int = 7,
    val intercourseEvents: List<IntercourseEvent> = emptyList()
)
