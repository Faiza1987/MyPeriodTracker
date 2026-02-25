package org.tracker.domain

import org.tracker.exceptions.InsufficientCycleHistoryException
import java.time.LocalDate

import java.time.temporal.ChronoUnit

data class CycleHistory(
    val cycles: List<Cycle>
) {
    fun cycleLengths(): List<Int> = cycles.sortedBy { it.startDate }.zipWithNext { a, b ->
        ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
    }

    fun averageCycleLength(): Int = cycleLengths().average().toInt()

    fun shortestCycle(): Int = cycleLengths().min()

    fun longestCycle(): Int = cycleLengths().max()

    fun stats(): CycleStats {
        val lengths = cycleLengths()
        val average = lengths.average()
        val variance = lengths.map { length ->
            (length - average) * (length - average)
        }.average()

        return CycleStats(
            average = average.toInt(),
            shortest = lengths.min(),
            longest = lengths.max(),
            standardDeviation = Math.sqrt(variance)
        )
    }

    fun predictNextCycleStartDate(): CyclePrediction {
        val averageLength = averageCycleLength()
        val lastStartDate = cycles.maxBy { it.startDate }.startDate

        return CyclePrediction(
            predictedStartDate = lastStartDate.plusDays(averageLength.toLong()),
            explanation = explainPrediction()
        )
    }

    fun canPredict(): Boolean {
        return cycles.size >= 2
    }

    fun isPredictionReliable(): Boolean {
        if (!canPredict()) return false
        val stats = stats()
        return stats.standardDeviation <= 3.0 && !hasRecentDisruptors()
    }

    fun predictNextCycleStart(): LocalDate {
        if (!canPredict()) throw InsufficientCycleHistoryException()
        return predictNextCycleStartDate().predictedStartDate
    }

    fun predictNextCycleWindow(): CyclePredictionWindow {
        val stats = stats()
        val lastStartDate = cycles.maxBy { it.startDate }.startDate

        val avg = stats.average.toLong()
        val deviation = stats.standardDeviation.toLong().coerceAtLeast(1)

        // Widen the window if recent cycles had disruptors
        val extra = if (hasRecentDisruptors()) 2L else 0L
        val predicted = lastStartDate.plusDays(avg)

        return CyclePredictionWindow(
            earliest = predicted.minusDays(deviation + extra),
            latest = predicted.plusDays(deviation + extra)
        )
    }

    fun explainPrediction(): PredictionExplanation {
        if (!canPredict()) {
            return PredictionExplanation(
                confidence = PredictionConfidence.LOW,
                reasons = listOf("Not enough cycle history to make a reliable prediction")
            )
        }

        val stats = stats()
        val disruptorReasons = disruptorReasons()
        val hasDisruptors = disruptorReasons.isNotEmpty()

        // Disruptors (high stress / illness) drop confidence by one level
        val baseConfidence = when {
            stats.standardDeviation <= 2.0 -> PredictionConfidence.HIGH
            stats.standardDeviation <= 5.0 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }

        val finalConfidence = if (hasDisruptors) baseConfidence.downgrade() else baseConfidence

        val reasons = buildList {
            add(
                when (baseConfidence) {
                    PredictionConfidence.HIGH -> "Cycle length has been very consistent"
                    PredictionConfidence.MEDIUM -> "Some variation in cycle length detected"
                    PredictionConfidence.LOW -> "Cycle length varies significantly between cycles"
                }
            )
            addAll(disruptorReasons)
        }

        return PredictionExplanation(confidence = finalConfidence, reasons = reasons)
    }

    fun predictNextCycle(): CyclePrediction {
        if (!canPredict()) throw InsufficientCycleHistoryException()

        val stats = stats()
        val predictedDate = predictNextCycleStartDate().predictedStartDate
        val disruptorReasons = disruptorReasons()
        val hasDisruptors = disruptorReasons.isNotEmpty()

        val baseConfidence = when {
            stats.standardDeviation <= 3.0 -> PredictionConfidence.HIGH
            stats.standardDeviation <= 6.0 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }

        val finalConfidence = if (hasDisruptors) baseConfidence.downgrade() else baseConfidence

        val reasons = buildList {
            addAll(predictionReasons(stats))
            addAll(disruptorReasons)
        }

        return CyclePrediction(
            predictedStartDate = predictedDate,
            explanation = PredictionExplanation(
                confidence = finalConfidence,
                reasons = reasons
            )
        )
    }

    /**
     * Checks the most recent cycle for known delay factors: high stress (4-5) or illness.
     */
    private fun hasRecentDisruptors(): Boolean {
        val lastCycle = cycles.maxByOrNull { it.startDate } ?: return false
        return lastCycle.isIll || (lastCycle.stressLevel != null && lastCycle.stressLevel >= 4)
    }

    /**
     * Returns human-readable reasons for any disruptors found in recent cycles.
     */
    private fun disruptorReasons(): List<String> {
        val lastCycle = cycles.maxByOrNull { it.startDate } ?: return emptyList()
        return buildList {
            if (lastCycle.isIll) {
                add("Illness was logged in the most recent cycle, which may delay the next period")
            }
            if (lastCycle.stressLevel != null && lastCycle.stressLevel >= 4) {
                add("High stress (level ${lastCycle.stressLevel}/5) was logged, which may affect cycle timing")
            }
        }
    }

    private fun predictionReasons(stats: CycleStats): List<String> {
        return buildList {
            add("Average cycle length: ${stats.average} days")
            add("Shortest cycle: ${stats.shortest} days")
            add("Longest cycle: ${stats.longest} days")
            add("Cycle variability (std dev): %.2f days".format(stats.standardDeviation))

            if (stats.standardDeviation <= 3.0) {
                add("Cycle history is consistent")
            } else {
                add("Cycle history shows high variability")
            }
        }
    }

    /**
     * Drops confidence by one level: HIGH -> MEDIUM, MEDIUM -> LOW, LOW stays LOW.
     */
    private fun PredictionConfidence.downgrade(): PredictionConfidence = when (this) {
        PredictionConfidence.HIGH -> PredictionConfidence.MEDIUM
        PredictionConfidence.MEDIUM -> PredictionConfidence.LOW
        PredictionConfidence.LOW -> PredictionConfidence.LOW
    }

}
