package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.tracker.models.CycleEntry
import org.tracker.models.PregnancyRisk
import org.tracker.models.UserProfile
import org.tracker.service.CycleCalculator
import java.time.LocalDate

class CycleCalculatorTest {
    private val cycleCalculator = CycleCalculator()

    @Test
    fun `returns day 1 when today is the first day of period`() {
        // Arrange
        val periodStart = LocalDate.of(2025, 1, 1)
        val today = LocalDate.of(2025, 1, 1)

        // Act
        val result = cycleCalculator.getCycleDay(today, periodStart)

        // Assert
        assertEquals(1, result)

    }

    @Test
    fun `returns day 2 for the day after period starts`() {
        // Arrange
        val periodStart = LocalDate.of(2025, 1, 1)
        val today = LocalDate.of(2025, 1, 2)

        // Act
        val result = cycleCalculator.getCycleDay(today, periodStart)

        // Assert
        assertEquals(2, result)
    }


    @Test
    fun `returns correct cycle day after multiple days`() {
        // Arrange
        val periodStart = LocalDate.of(2025, 1, 1)
        val today = LocalDate.of(2025, 1, 10)

        // Act
        val result = cycleCalculator.getCycleDay(today, periodStart)

        // Assert
        assertEquals(10, result)
    }


    @Test
    fun `handles larger gaps correctly`() {
        // Arrange
        val periodStart = LocalDate.of(2025, 1, 1)
        val today = LocalDate.of(2025, 1, 28)

        // Act
        val result = cycleCalculator.getCycleDay(today, periodStart)

        // Assert
        assertEquals(28, result)
    }

    @Test
    fun `fertile window is calculated correctly for 28 day cycle`() {
        // Arrange
        val lastPeriodStart = LocalDate.of(2025, 1, 1)
        val averageCycleLength = 28

        // Act
        val (fertileStart, fertileEnd) =
            cycleCalculator.calculateFertileWindow(lastPeriodStart, averageCycleLength)

        // Assert
        // Ovulation = Day 14 → Jan 14
        // Fertile window = Jan 9 – Jan 14
        assertEquals(LocalDate.of(2025, 1, 9), fertileStart)
        assertEquals(LocalDate.of(2025, 1, 14), fertileEnd)
    }

    @Test
    fun `fertile window shifts correctly for longer cycle`() {
        // Arrange
        val lastPeriodStart = LocalDate.of(2025, 1, 1)
        val averageCycleLength = 32

        // Act
        val (fertileStart, fertileEnd) =
            cycleCalculator.calculateFertileWindow(lastPeriodStart, averageCycleLength)

        // Assert
        // Ovulation = Day 18 → Jan 18
        // Fertile window = Jan 13 – Jan 18
        assertEquals(LocalDate.of(2025, 1, 13), fertileStart)
        assertEquals(LocalDate.of(2025, 1, 18), fertileEnd)
    }

    @Test
    fun `fertile window shifts correctly for shorter cycle`() {
        // Arrange
        val lastPeriodStart = LocalDate.of(2025, 1, 1)
        val averageCycleLength = 24

        // Act
        val (fertileStart, fertileEnd) =
            cycleCalculator.calculateFertileWindow(lastPeriodStart, averageCycleLength)

        // Assert
        // Ovulation = Day 10 → Jan 10
        // Fertile window = Jan 5 – Jan 10
        assertEquals(LocalDate.of(2025, 1, 5), fertileStart)
        assertEquals(LocalDate.of(2025, 1, 10), fertileEnd)
    }

    @Test
    fun `returns HIGH risk when today is in fertile window and intercourse occurred`() {
        // Arrange
        val fertileStart = LocalDate.of(2025, 1, 9)
        val fertileEnd = LocalDate.of(2025, 1, 14)
        val fertileWindow = fertileStart to fertileEnd

        val today = LocalDate.of(2025, 1, 12)
        val intercourseDays = listOf(LocalDate.of(2025, 1, 11))

        // Act
        val result = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseDays = intercourseDays
        )

        // Assert
        assertEquals(PregnancyRisk.HIGH, result)
    }

    @Test
    fun `returns MEDIUM risk when today is in fertile window but no intercourse occurred`() {
        // Arrange
        val fertileStart = LocalDate.of(2025, 1, 9)
        val fertileEnd = LocalDate.of(2025, 1, 14)
        val fertileWindow = fertileStart to fertileEnd

        val today = LocalDate.of(2025, 1, 11)
        val intercourseDays = emptyList<LocalDate>()

        // Act
        val result = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseDays = intercourseDays
        )

        // Assert
        assertEquals(PregnancyRisk.MEDIUM, result)
    }

    @Test
    fun `returns LOW risk when today is outside fertile window`() {
        // Arrange
        val fertileStart = LocalDate.of(2025, 1, 9)
        val fertileEnd = LocalDate.of(2025, 1, 14)
        val fertileWindow = fertileStart to fertileEnd

        val today = LocalDate.of(2025, 1, 20)
        val intercourseDays = listOf(LocalDate.of(2025, 1, 11))

        // Act
        val result = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseDays = intercourseDays
        )

        // Assert
        assertEquals(PregnancyRisk.LOW, result)
    }

    @Test
    fun `generateCycleSummary returns correct values`() {
        val today = LocalDate.of(2025, 1, 12)

        val cycle = CycleEntry(
            periodStart = LocalDate.of(2025, 1, 1),
            unprotectedIntercourseDays = listOf(LocalDate.of(2025, 1, 11))
        )

        val user = UserProfile(
            averageCycleLength = 28,
            cycles = listOf(cycle)
        )

        val summary = cycleCalculator.generateCycleSummary(today, user)

        assertEquals(12, summary.cycleDay)
        assertEquals(
            LocalDate.of(2025, 1, 9),
            summary.fertileWindow.first
        )
        assertEquals(
            LocalDate.of(2025, 1, 14),
            summary.fertileWindow.second
        )
        assertEquals(PregnancyRisk.HIGH, summary.pregnancyRisk)
    }


}