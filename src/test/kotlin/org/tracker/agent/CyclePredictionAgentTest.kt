package org.tracker.agent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.tracker.domain.PredictionConfidence
import java.time.LocalDate
import java.util.UUID

class CyclePredictionAgentTest {

    companion object {
        private val TEST_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001")

    }


    @Test
    fun `uses CycleHistory confidence when no prior predictions exist`() {
        val agent = CyclePredictionAgent(
            userId = TEST_USER_ID,
            memoryStore = InMemoryCyclePredictionMemoryStore()
        )


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
        val agent = CyclePredictionAgent(
            userId = TEST_USER_ID,
            memoryStore = InMemoryCyclePredictionMemoryStore()
        )


        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.recordPeriod(LocalDate.of(2025, 2, 26))

        // First accurate prediction
        var prediction = agent.predictNextCycle()
        agent.updateWithActualPeriodStartDate(prediction.predictedStartDate)

        // Second accurate prediction (this enables learning)
        prediction = agent.predictNextCycle()
        agent.updateWithActualPeriodStartDate(prediction.predictedStartDate)

        val nextPrediction = agent.predictNextCycle()

        assertEquals(
            PredictionConfidence.HIGH,
            nextPrediction.explanation.confidence
        )
    }


    @Test
    fun `learned MEDIUM confidence when past predictions were moderately inaccurate`() {
        val agent = CyclePredictionAgent(
            userId = TEST_USER_ID,
            memoryStore = InMemoryCyclePredictionMemoryStore()
        )


        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.recordPeriod(LocalDate.of(2025, 2, 26))

        // First moderately inaccurate prediction
        var prediction = agent.predictNextCycle()
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate.plusDays(2)
        )

        // Second moderately inaccurate prediction
        prediction = agent.predictNextCycle()
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
        val agent = CyclePredictionAgent(
            userId = TEST_USER_ID,
            memoryStore = InMemoryCyclePredictionMemoryStore()
        )


        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.recordPeriod(LocalDate.of(2025, 2, 26))

        // First bad prediction
        var prediction = agent.predictNextCycle()
        agent.updateWithActualPeriodStartDate(
            prediction.predictedStartDate.plusDays(7)
        )

        // Second bad prediction
        prediction = agent.predictNextCycle()
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
        val agent = CyclePredictionAgent(
            userId = TEST_USER_ID,
            memoryStore = InMemoryCyclePredictionMemoryStore()
        )


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
            PredictionConfidence.MEDIUM,
            finalPrediction.explanation.confidence
        )
    }
}