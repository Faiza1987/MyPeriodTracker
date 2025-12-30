package org.tracker.service

import org.tracker.model.CycleSummary
import org.tracker.models.PregnancyRisk
import org.tracker.models.UserProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        intercourseDays: List<LocalDate>,
    ) : PregnancyRisk {
        val (fertileStart, fertileEnd) = fertileWindow

        val isInFertileWindow = !today.isBefore(fertileStart) && !today.isAfter(fertileEnd)

        val hadUnprotectedSex = intercourseDays.any {
            !it.isBefore(fertileStart) && !it.isAfter(fertileEnd)
        }

        return when {
            isInFertileWindow && hadUnprotectedSex -> PregnancyRisk.HIGH
            isInFertileWindow -> PregnancyRisk.MEDIUM
            else -> PregnancyRisk.LOW
        }
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

        val risk = calculatePregnancyRisk(
            today,
            fertileWindow,
            lastCycle.unprotectedIntercourseDays
        )

        return CycleSummary(
            cycleDay = cycleDay,
            fertileWindow = fertileWindow,
            pregnancyRisk = risk
        )
    }

}