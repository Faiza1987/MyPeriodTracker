package org.tracker.domain

import java.time.LocalDate

data class CyclePredictionWindow(
    val earliest: LocalDate,
    val latest: LocalDate,
)
