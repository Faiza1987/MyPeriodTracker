package org.tracker.domain

data class CycleStats(
    val average: Int,
    val shortest: Int,
    val longest: Int,
    val standardDeviation: Double,
)
