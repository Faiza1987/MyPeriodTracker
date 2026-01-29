package org.tracker.domain

data class PregnancyRiskKey(
    val inFertileWindow: Boolean,
    val unprotectedIntercourseInWindow: Boolean
)