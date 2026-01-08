package org.tracker.service


import org.springframework.stereotype.Service
import org.tracker.model.CycleSummary
import org.tracker.model.IntercourseEvent
import org.tracker.model.PregnancyRiskAssessment
import org.tracker.model.PregnancyRiskKey
import org.tracker.models.PregnancyRisk
import org.tracker.models.UserProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class CycleCalculator{
    fun getCycleDay(
        today: LocalDate,
        lastPeriodStart: LocalDate
    ) : Int {
        return ChronoUnit.DAYS.between(lastPeriodStart, today).toInt() + 1
    }

    fun calculateFertileWindow(
        lastPeriodStart: LocalDate,
        averageCycleLength: Int
    ): Pair<LocalDate, LocalDate> {
        val ovulationDay = lastPeriodStart.plusDays(
            (averageCycleLength - 14 - 1).toLong()
        )

        return ovulationDay.minusDays(5) to ovulationDay
    }

    fun calculatePregnancyRisk(
        today: LocalDate,
        fertileWindow: Pair<LocalDate, LocalDate>,
        intercourseEvents: List<IntercourseEvent>,
    ) : PregnancyRiskAssessment {

        val inFertileWindow = isInFertileWindow(today, fertileWindow)
        val unprotectedIntercourseInWindow = hadUnprotectedIntercourseInWindow(intercourseEvents, fertileWindow)

        return assessPregnancyRisk(
            inFertileWindow = inFertileWindow,
            unprotectedIntercourseInWindow = unprotectedIntercourseInWindow
        )
    }

    fun generateCycleSummary(
        today: LocalDate,
        user: UserProfile
    ): CycleSummary {

        val lastCycle = user.cycles.last()
        val cycleDay = getCycleDay(today, lastCycle.periodStart)
        val fertileWindow = calculateFertileWindow(
            lastCycle.periodStart,
            user.averageCycleLength
        )

        val pregnancyRiskAssessment = calculatePregnancyRisk(
            today,
            fertileWindow,
            lastCycle.intercourseEvents
        )

        return CycleSummary(
            cycleDay = cycleDay,
            fertileWindow = fertileWindow,
            pregnancyRiskAssessment = pregnancyRiskAssessment
        )
    }

    private fun isInFertileWindow(
        today: LocalDate,
        fertileWindow: Pair<LocalDate, LocalDate>,
    ) : Boolean {
        val (start, end) = fertileWindow
        return !today.isBefore(start) && !today.isAfter(end)
    }


    internal fun assessPregnancyRisk(
        inFertileWindow: Boolean,
        unprotectedIntercourseInWindow: Boolean,
    ): PregnancyRiskAssessment {
        val key = PregnancyRiskKey(
            inFertileWindow = inFertileWindow,
            unprotectedIntercourseInWindow = unprotectedIntercourseInWindow
        )

        return pregnancyRiskTable[key]
            ?: error("No pregnancy risk rule defined for $key")
    }

    internal fun pregnancyRiskDecision(
        inFertileWindow: Boolean,
        unprotectedIntercourseInWindow: Boolean
    ): PregnancyRisk {
        return assessPregnancyRisk(
            inFertileWindow,
            unprotectedIntercourseInWindow
        ).risk
    }



    private val pregnancyRiskTable: Map<PregnancyRiskKey, PregnancyRiskAssessment> =
        mapOf(
            PregnancyRiskKey(true, true) to PregnancyRiskAssessment(
                risk = PregnancyRisk.HIGH,
                reasons = listOf(
                    "Today is within the fertile window",
                    "Unprotected intercourse occurred during the fertile window"
                )
            ),
            PregnancyRiskKey(true, false) to PregnancyRiskAssessment(
                risk = PregnancyRisk.MEDIUM,
                reasons = listOf(
                    "Today is within the fertile window",
                    "No unprotected intercourse occurred during the fertile window"
                )
            ),
            PregnancyRiskKey(false, true) to PregnancyRiskAssessment(
                risk = PregnancyRisk.LOW,
                reasons = listOf(
                    "Unprotected intercourse occurred outside the fertile window"
                )
            ),
            PregnancyRiskKey(false, false) to PregnancyRiskAssessment(
                risk = PregnancyRisk.LOW,
                reasons = listOf(
                    "No unprotected intercourse occurred during the fertile window"
                )
            )
        )


    private fun hadUnprotectedIntercourseInWindow(
        events: List<IntercourseEvent>,
        fertileWindow: Pair<LocalDate, LocalDate>,
    ): Boolean {
        val (start, end) = fertileWindow
        return events.any { event ->
            !event.protected &&
                    !event.date.isBefore(start) &&
                    !event.date.isAfter(end)
        }
    }


}