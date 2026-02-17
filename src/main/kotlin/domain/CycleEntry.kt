package org.tracker.domain

import java.time.LocalDate

data class CycleEntry(
    val periodStart: LocalDate,
    val periodDuration: Int = 7,
    val intercourseEvents: List<IntercourseEvent> = emptyList(),
    val flowIntensity: String? = null,
    val cervicalMucus: String? = null,
    val stressLevel: Int? = null,
    val isIll: Boolean = false,
    val notes: String? = null
)