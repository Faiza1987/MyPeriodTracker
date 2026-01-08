package org.tracker.model

import org.tracker.models.PregnancyRisk

data class PregnancyRiskAssessment(
    val risk: PregnancyRisk,
    val reasons: List<String>
)
