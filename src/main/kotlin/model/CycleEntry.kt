package org.tracker.models

import java.time.LocalDate


data class CycleEntry(
    val periodStart: LocalDate,
    val periodDuration: Int = 7,
    val unprotectedIntercourseDays: List<LocalDate> = emptyList()
)
