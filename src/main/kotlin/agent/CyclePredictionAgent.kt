package org.tracker.agent

import org.tracker.domain.Cycle
import org.tracker.domain.CycleHistory
import org.tracker.domain.CyclePrediction
import org.tracker.domain.PredictionConfidence
import org.tracker.domain.PredictionExplanation
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CyclePredictionAgent {
    private val cycles = mutableListOf<Cycle>()
    private val predictionResult = mutableListOf<CyclePredictionResult>()
    private var lastPrediction: CyclePrediction? = null


    // Records the start of a new menstrual cycle
    fun recordPeriod(startDate: LocalDate) {
        cycles.add(Cycle(startDate))
    }

    // Predicts the next cycle using current history
    fun predictNextCycle(): CyclePrediction {
        val history = CycleHistory(
            cycles = cycles.toList()
        )
        val basePrediction = history.predictNextCycle()
        val learnedConfidence = learnedConfidence()
        val finalConfidence = learnedConfidence ?: basePrediction.explanation.confidence
        val reasons = buildList {
            addAll(basePrediction.explanation.reasons)

            if(learnedConfidence != null) {
                add(
                    when(learnedConfidence) {
                        PredictionConfidence.HIGH -> "Past predictions have been very accurate"
                        PredictionConfidence.MEDIUM -> "Past predictions have been moderately accurate"
                        PredictionConfidence.LOW -> "Past predictions have been inaccurate"

                    }
                )
            }
        }
        return CyclePrediction(
            predictedStartDate = basePrediction.predictedStartDate,
            explanation = PredictionExplanation(
                confidence = finalConfidence,
                reasons = reasons
            )
        )

    }

    // Used later for learning from prediction accuracy
    fun updateWithActualPeriodStartDate(actualStartDate: LocalDate) {
        val prediction = lastPrediction ?: return

        val difference = ChronoUnit.DAYS.between(
            prediction.predictedStartDate,
            actualStartDate
        )

        predictionResult.add(
            CyclePredictionResult(
                predictedDate = prediction.predictedStartDate,
                actualDate = actualStartDate,
                differenceInDays = difference.toInt()
            )
        )

        // also record the actual period
        recordPeriod(actualStartDate)
    }

    fun predictionResults(): List<CyclePredictionResult> {
        return predictionResult.toList()
    }

    private fun learnedConfidence(): PredictionConfidence? {
        if (predictionResult.isEmpty()) return null

        val averageError = predictionResult
            .takeLast(3) // recent history matters most
            .map { kotlin.math.abs(it.differenceInDays) }
            .average()

        return when {
            averageError <= 1 -> PredictionConfidence.HIGH
            averageError <= 3 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }
    }

}