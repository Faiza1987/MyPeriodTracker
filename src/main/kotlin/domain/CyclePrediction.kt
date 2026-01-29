package org.tracker.domain

import java.time.LocalDate

data class CyclePrediction(
    val predictedStartDate: LocalDate,
    val explanation: PredictionExplanation
)

