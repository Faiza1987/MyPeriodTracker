package org.tracker.domain

data class PregnancyRiskRule(
    val inFertileWindow: Boolean,
    val intercourseInWindow: Boolean,
    val risk: PregnancyRisk
)