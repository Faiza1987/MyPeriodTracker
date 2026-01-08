package org.tracker.api.dto

import org.tracker.models.UserProfile
import java.time.LocalDate

data class CycleSummaryRequest(
    val today: LocalDate,
    val user: UserProfile
)
