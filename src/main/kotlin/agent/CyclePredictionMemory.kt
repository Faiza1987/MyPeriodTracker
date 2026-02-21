package org.tracker.agent

import org.tracker.domain.Cycle
import org.tracker.domain.CyclePrediction
import java.time.LocalDate

/*
This is a pure data holder.
No prediction logic. No learning logic.
*/
class CyclePredictionMemory(
    val cycles: List<Cycle> = emptyList(),
    val predictionResults: List<CyclePredictionResult> = emptyList(),
    val lastPrediction: CyclePrediction? = null
) {
    fun recordCycle(cycle: Cycle): CyclePredictionMemory =
        copy(cycles = cycles + cycle)

    fun recordPredictionResult(
        result: CyclePredictionResult
    ): CyclePredictionMemory =
        copy(predictionResults = predictionResults + result)

    fun withLastPrediction(
        prediction: CyclePrediction
    ): CyclePredictionMemory =
        copy(lastPrediction = prediction)

    private fun copy(
        cycles: List<Cycle> = this.cycles,
        predictionResults: List<CyclePredictionResult> = this.predictionResults,
        lastPrediction: CyclePrediction? = this.lastPrediction
    ) = CyclePredictionMemory(
        cycles = cycles,
        predictionResults = predictionResults,
        lastPrediction = lastPrediction
    )
}