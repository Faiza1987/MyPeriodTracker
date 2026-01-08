package org.tracker.model

import java.time.LocalDate

data class IntercourseEvent(
    val date: LocalDate,
    val protected: Boolean,
)
