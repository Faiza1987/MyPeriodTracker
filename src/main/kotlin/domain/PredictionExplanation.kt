package org.tracker.domain

data class PredictionExplanation(
    val confidence: PredictionConfidence,
    val reasons: List<String>
)
