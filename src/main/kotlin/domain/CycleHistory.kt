package org.tracker.domain

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
        return stats.standardDeviation <= 3.0
    }

    fun predictNextCycleStart(): LocalDate {
        if (!canPredict()) {
            throw IllegalStateException("Not enough cycle data to make a prediction")
        }

        return predictNextCycleStartDate().predictedStartDate
    }


    fun predictNextCycleWindow(): CyclePredictionWindow {
        val stats = stats()
        val lastStartDate = cycles.maxBy { it.startDate }.startDate

        val avg = stats.average.toLong()
        val deviation = stats.standardDeviation.toLong().coerceAtLeast(1)

        val predicted = lastStartDate.plusDays(avg)

        return CyclePredictionWindow(
            earliest = predicted.minusDays(deviation),
            latest = predicted.plusDays(deviation)
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

        return when {
            stats.standardDeviation <= 2.0 ->
                PredictionExplanation(
                    confidence = PredictionConfidence.HIGH,
                    reasons = listOf("Cycle length has been very consistent")
                )

            stats.standardDeviation <= 5.0 ->
                PredictionExplanation(
                    confidence = PredictionConfidence.MEDIUM,
                    reasons = listOf("Some variation in cycle length detected")
                )

            else ->
                PredictionExplanation(
                    confidence = PredictionConfidence.LOW,
                    reasons = listOf("Cycle length varies significantly between cycles")
                )
        }
    }

    fun predictNextCycle(): CyclePrediction {
        if (!canPredict()) {
            throw IllegalStateException("Not enough data to predict next cycle")
        }

        val stats = stats()
        val predictedDate = predictNextCycleStartDate().predictedStartDate

        val confidence =
            when {
                stats.standardDeviation <= 3.0 -> PredictionConfidence.HIGH
                stats.standardDeviation <= 6.0 -> PredictionConfidence.MEDIUM
                else -> PredictionConfidence.LOW
            }

        return CyclePrediction(
            predictedStartDate = predictedDate,
            explanation = PredictionExplanation(
                confidence = confidence,
                reasons = predictionReasons(stats)
            )
        )
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



}
