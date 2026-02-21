package org.tracker.api.dto

import java.time.LocalDate
import java.util.UUID

data class RecordPeriodRequest(
    val userId: UUID,
    val periodStart: LocalDate,
    val periodDuration: Int = 7,
    val flowIntensity: String? = null,
    val cervicalMucus: String? = null,
    val stressLevel: Int? = null,
    val isIll: Boolean = false,
    val notes: String? = null
)