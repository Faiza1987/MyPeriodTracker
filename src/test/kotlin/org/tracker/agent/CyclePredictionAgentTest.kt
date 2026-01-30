package org.tracker.agent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.tracker.domain.PredictionConfidence
import java.time.LocalDate

class CyclePredictionAgentTest {

    @Test
    fun `uses CycleHistory confidence when no prior predictions exist`() {
        val agent = CyclePredictionAgent()

        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29)) // 28 days

        val prediction = agent.predictNextCycle()

        assertEquals(
            PredictionConfidence.HIGH,
            prediction.explanation.confidence
        )
    }

    @Test
    fun `learned HIGH confidence when past predictions were very accurate`() {
        val agent = CyclePredictionAgent()

        // Build consistent cycle history
        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.recordPeriod(LocalDate.of(2025, 2, 26))

        // First prediction
        val prediction = agent.predictNextCycle()

        // Actual date matches prediction exactly
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate
        )

        val nextPrediction = agent.predictNextCycle()

        assertEquals(
            PredictionConfidence.HIGH,
            nextPrediction.explanation.confidence
        )
    }

    @Test
    fun `learned MEDIUM confidence when past predictions were moderately inaccurate`() {
        val agent = CyclePredictionAgent()

        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.recordPeriod(LocalDate.of(2025, 2, 26))

        val prediction = agent.predictNextCycle()

        // Off by 2 days
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate.plusDays(2)
        )

        val nextPrediction = agent.predictNextCycle()

        assertEquals(
            PredictionConfidence.MEDIUM,
            nextPrediction.explanation.confidence
        )
    }

    @Test
    fun `learned LOW confidence when past predictions were very inaccurate`() {
        val agent = CyclePredictionAgent()

        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.recordPeriod(LocalDate.of(2025, 2, 26))

        val prediction = agent.predictNextCycle()

        // Off by 7 days
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate.plusDays(7)
        )

        val nextPrediction = agent.predictNextCycle()

        assertEquals(
            PredictionConfidence.LOW,
            nextPrediction.explanation.confidence
        )
    }

    @Test
    fun `recent predictions matter more than older ones`() {
        val agent = CyclePredictionAgent()

        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.recordPeriod(LocalDate.of(2025, 2, 26))

        // First bad prediction
        var prediction = agent.predictNextCycle()
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate.plusDays(6)
        )

        // Second accurate prediction
        prediction = agent.predictNextCycle()
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate
        )

        // Third accurate prediction
        prediction = agent.predictNextCycle()
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate
        )

        val finalPrediction = agent.predictNextCycle()

        assertEquals(
            PredictionConfidence.HIGH,
            finalPrediction.explanation.confidence
        )
    }
}