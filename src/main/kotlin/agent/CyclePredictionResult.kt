package org.tracker.agent

import java.time.LocalDate

data class CyclePredictionResult(
    val predictedDate: LocalDate,
    val actualDate: LocalDate,
    val differenceInDays: Int
)
