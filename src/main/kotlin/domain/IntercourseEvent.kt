package org.tracker.domain

import java.time.LocalDate

data class IntercourseEvent(
    val date: LocalDate,
    val protected: Boolean,
)