package org.tracker.api.dto

import org.tracker.domain.CyclePrediction

data class PredictionResponse(
    val predictedStartDate: String,
    val confidence: String,
    val reasons: List<String>
) {
    companion object {
        fun from(prediction: CyclePrediction): PredictionResponse =
            PredictionResponse(
                predictedStartDate = prediction.predictedStartDate.toString(),
                confidence = prediction.explanation.confidence.name,
                reasons = prediction.explanation.reasons
            )
    }
}