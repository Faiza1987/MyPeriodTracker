package org.tracker.model

import org.tracker.models.PregnancyRisk

data class PregnancyRiskRule(
    val inFertileWindow: Boolean,
    val intercourseInWindow: Boolean,
    val risk: PregnancyRisk
)
