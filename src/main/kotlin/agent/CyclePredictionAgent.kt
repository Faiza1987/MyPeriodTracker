package org.tracker.agent

import org.tracker.domain.Cycle
import org.tracker.domain.CycleHistory
import org.tracker.domain.CyclePrediction
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
        val prediction = history.predictNextCycle()
        lastPrediction = prediction

        return prediction
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
}