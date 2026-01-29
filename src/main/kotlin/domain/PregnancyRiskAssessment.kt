package org.tracker.domain

data class PregnancyRiskAssessment(
    val risk: PregnancyRisk,
    val reasons: List<String>
)