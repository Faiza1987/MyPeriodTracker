package org.tracker

import org.tracker.api.dto.CycleSummaryRequest
import org.tracker.domain.*
import java.time.LocalDate

object TestFixtures {

    fun cycleSummaryRequest(
        today: LocalDate = LocalDate.of(2025, 1, 12)
    ): CycleSummaryRequest =
        CycleSummaryRequest(
            today = today,
            user = userProfile()
        )

    fun userProfile(): UserProfile =
        UserProfile(
            averageCycleLength = 28,
            cycles = listOf(
                CycleEntry(
                    periodStart = LocalDate.of(2025, 1, 1),
                    intercourseEvents = listOf(
                        IntercourseEvent(
                            date = LocalDate.of(2025, 1, 11),
                            protected = false
                        )
                    )
                )
            )
        )

    fun cycleSummary(): CycleSummary =
        CycleSummary(
            cycleDay = 12,
            fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14),
            pregnancyRiskAssessment = PregnancyRiskAssessment(
                risk = PregnancyRisk.HIGH,
                reasons = listOf(
                    "Today is within the fertile window",
                    "Unprotected intercourse occurred during the fertile window"
                )
            )
        )
}