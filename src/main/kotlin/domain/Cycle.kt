package org.tracker.domain

import java.time.LocalDate

data class Cycle(
    val startDate: LocalDate,
    val stressLevel: Int? = null,
    val isIll: Boolean = false,
    val flowIntensity: String? = null,
    val cervicalMucus: String? = null,
    val notes: String? = null,
)
