package org.tracker.models

data class UserProfile(
    val averageCycleLength: Int = 28,
    val typicalPeriodLength: Int = 7,
    val cycles: List<CycleEntry> = emptyList(),
)
