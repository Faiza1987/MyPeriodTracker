package org.tracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import java.time.LocalDate

class CycleHistoryTest {

    @Test
    fun `calculates average cycle length`() {
        // Arrange & Act
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)),
                Cycle(LocalDate.of(2026, 2, 26))
            )
        )
        // Assert
        assertEquals(28, history.averageCycleLength())
    }

    @Test
    fun `calculates shortest cycle`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)), // 28 days
                Cycle(LocalDate.of(2026, 2, 24))  // 26 days
            )
        )

        assertEquals(26, history.shortestCycle())
    }

    @Test
    fun `calculates longest cycle`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)), // 28 days
                Cycle(LocalDate.of(2026, 2, 26))  // 28 days
            )
        )

        assertEquals(28, history.longestCycle())
    }

    @Test
    fun `predicts next cycle start date`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)),
                Cycle(LocalDate.of(2026, 2, 26))
            )
        )

        val prediction = history.predictNextCycleStartDate()

        assertEquals(
            LocalDate.of(2026, 3, 26),
            prediction.predictedStartDate
        )
    }

    @Test
    fun `cannot predict with fewer than 2 cycles`() {
        val history = CycleHistory(
            listOf(Cycle(LocalDate.of(2026, 1, 1)))
        )

        assertEquals(false, history.canPredict())
    }

    @Test
    fun `prediction is reliable for consistent cycles`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)),
                Cycle(LocalDate.of(2026, 2, 26))
            )
        )

        assertEquals(true, history.isPredictionReliable())
    }

    @Test
    fun `predicts a confidence window for next cycle`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)),
                Cycle(LocalDate.of(2026, 2, 26))
            )
        )

        val window = history.predictNextCycleWindow()

        assertEquals(
            LocalDate.of(2026, 3, 25),
            window.earliest
        )
        assertEquals(
            LocalDate.of(2026, 3, 27),
            window.latest
        )
    }
    @Test
    fun `calculates average cycle length with variation`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)), // 28
                Cycle(LocalDate.of(2026, 2, 24))  // 26
            )
        )

        assertEquals(27, history.averageCycleLength())
    }

    @Test
    fun `prediction is not reliable for irregular cycles`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),  // —
                Cycle(LocalDate.of(2026, 1, 21)), // 20
                Cycle(LocalDate.of(2026, 2, 25))  // 35
            )
        )

        assertEquals(false, history.isPredictionReliable())
    }
    @Test
    fun `prediction window is wider for irregular cycles`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 21)), // 20
                Cycle(LocalDate.of(2026, 2, 25))  // 35
            )
        )

        val window = history.predictNextCycleWindow()

        assert(window.latest.isAfter(window.earliest))
    }

    @Test
    fun `predicts next cycle with high confidence for consistent cycles`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)),
                Cycle(LocalDate.of(2026, 2, 26))
            )
        )

        val prediction = history.predictNextCycle()

        assertEquals(
            LocalDate.of(2026, 3, 26),
            prediction.predictedStartDate
        )
        assertEquals(
            PredictionConfidence.HIGH,
            prediction.explanation.confidence
        )
    }

    @Test
    fun `predictNextCycleStart returns predicted start date`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29))
            )
        )

        val predicted = history.predictNextCycleStart()

        assertEquals(LocalDate.of(2026, 2, 26), predicted)
    }

    @Test
    fun `predictNextCycle returns high confidence for consistent cycles with different length`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 30)), // 29
                Cycle(LocalDate.of(2026, 2, 28))  // 29
            )
        )

        val prediction = history.predictNextCycle()

        assertEquals(PredictionConfidence.HIGH, prediction.explanation.confidence)
    }

    @Test
    fun `predictNextCycle returns low confidence for highly irregular cycles`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 20)), // 19
                Cycle(LocalDate.of(2026, 2, 25))  // 36
            )
        )

        val prediction = history.predictNextCycle()

        assertEquals(PredictionConfidence.LOW, prediction.explanation.confidence)
    }

    @Test
    fun `prediction explanation includes descriptive reasons`() {
        val history = CycleHistory(
            listOf(
                Cycle(LocalDate.of(2026, 1, 1)),
                Cycle(LocalDate.of(2026, 1, 29)),
                Cycle(LocalDate.of(2026, 2, 26))
            )
        )

        val explanation = history.predictNextCycle().explanation

        assertTrue(explanation.reasons.any { it.contains("Average cycle length") })
        assertTrue(explanation.reasons.any { it.contains("Shortest cycle") })
        assertTrue(explanation.reasons.any { it.contains("Longest cycle") })
    }


}