package org.tracker.api.dto

import java.time.LocalDate
import java.util.UUID

data class ConfirmActualPeriodRequest(
    val userId: UUID,
    val actualStartDate: LocalDate
)