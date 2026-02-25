package org.tracker.api.dto

import org.tracker.domain.UserProfile
import java.util.UUID

data class AIPredictionRequest(
    val userId: UUID,
    val userProfile: UserProfile
)
