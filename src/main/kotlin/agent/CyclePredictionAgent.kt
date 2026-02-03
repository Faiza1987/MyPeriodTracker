package org.tracker.agent

import org.tracker.domain.Cycle
import org.tracker.domain.CycleHistory
import org.tracker.domain.CyclePrediction
import org.tracker.domain.PredictionConfidence
import org.tracker.domain.PredictionExplanation
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CyclePredictionAgent(
    private val memoryStore: CyclePredictionMemoryStore
) {

    private fun memory(): CyclePredictionMemory =
        memoryStore.load()

    private fun save(memory: CyclePredictionMemory) =
        memoryStore.save(memory)

    fun recordPeriod(startDate: LocalDate) {
        val updated = memory().recordCycle(startDate)
        save(updated)
    }

    fun predictNextCycle(): CyclePrediction {
        val memory = memory()

        val history = CycleHistory(
            cycles = memory.cycles
        )

        val basePrediction = history.predictNextCycle()
        val learnedConfidence = learnedConfidence(memory)
        val finalConfidence =
            learnedConfidence ?: basePrediction.explanation.confidence

        val reasons = buildList {
            addAll(basePrediction.explanation.reasons)

            if (learnedConfidence != null) {
                add(
                    when (learnedConfidence) {
                        PredictionConfidence.HIGH ->
                            "Past predictions have been very accurate"
                        PredictionConfidence.MEDIUM ->
                            "Past predictions have been moderately accurate"
                        PredictionConfidence.LOW ->
                            "Past predictions have been inaccurate"
                    }
                )
            }
        }

        val finalPrediction = CyclePrediction(
            predictedStartDate = basePrediction.predictedStartDate,
            explanation = PredictionExplanation(
                confidence = finalConfidence,
                reasons = reasons
            )
        )

        save(memory.withLastPrediction(finalPrediction))
        return finalPrediction
    }

    fun updateWithActualPeriodStartDate(actualStartDate: LocalDate) {
        val memory = memory()
        val prediction = memory.lastPrediction ?: return

        val difference = ChronoUnit.DAYS.between(
            prediction.predictedStartDate,
            actualStartDate
        )

        val result = CyclePredictionResult(
            predictedDate = prediction.predictedStartDate,
            actualDate = actualStartDate,
            differenceInDays = difference.toInt()
        )

        save(
            memory
                .recordPredictionResult(result)
                .recordCycle(actualStartDate)
        )
    }

    fun predictionResults(): List<CyclePredictionResult> =
        memory().predictionResults

    private fun learnedConfidence(
        memory: CyclePredictionMemory
    ): PredictionConfidence? {
        if (memory.predictionResults.size < 2) return null

        val averageError = memory.predictionResults
            .takeLast(3)
            .map { kotlin.math.abs(it.differenceInDays) }
            .average()

        return when {
            averageError <= 1 -> PredictionConfidence.HIGH
            averageError <= 3 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }
    }
}
