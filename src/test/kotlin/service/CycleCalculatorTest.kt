package service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.tracker.model.IntercourseEvent
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
        val intercourseEvents = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = false
            )
        )


        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseEvents
        )

        // Assert
        assertEquals(PregnancyRisk.HIGH, assessment.risk)
    }

    @Test
    fun `returns MEDIUM risk when today is in fertile window but no intercourse occurred`() {
        // Arrange
        val fertileStart = LocalDate.of(2025, 1, 9)
        val fertileEnd = LocalDate.of(2025, 1, 14)
        val fertileWindow = fertileStart to fertileEnd

        val today = LocalDate.of(2025, 1, 11)
        val intercourseDays = emptyList<IntercourseEvent>()

        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseDays
        )

        // Assert
        assertEquals(PregnancyRisk.MEDIUM, assessment.risk)
    }

    @Test
    fun `returns LOW risk when today is outside fertile window`() {
        // Arrange
        val fertileStart = LocalDate.of(2025, 1, 9)
        val fertileEnd = LocalDate.of(2025, 1, 14)
        val fertileWindow = fertileStart to fertileEnd

        val today = LocalDate.of(2025, 1, 20)
        val intercourseEvents = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = false
            )
        )

        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseEvents
        )

        // Assert
        assertEquals(PregnancyRisk.LOW, assessment.risk)
    }

    @Test
    fun `generateCycleSummary returns correct values`() {
        // Arrange
        val today = LocalDate.of(2025, 1, 12)

        val cycle = CycleEntry(
            periodStart = LocalDate.of(2025, 1, 1),
            intercourseEvents = listOf(
                IntercourseEvent(
                    date = LocalDate.of(2025, 1, 11),
                    protected = false
                )
            )
        )

        val user = UserProfile(
            averageCycleLength = 28,
            cycles = listOf(cycle)
        )

        // Act
        val summary = cycleCalculator.generateCycleSummary(today, user)

        // Assert
        assertEquals(12, summary.cycleDay)
        assertEquals(
            LocalDate.of(2025, 1, 9),
            summary.fertileWindow.first
        )
        assertEquals(
            LocalDate.of(2025, 1, 14),
            summary.fertileWindow.second
        )
        assertEquals(PregnancyRisk.HIGH, summary.pregnancyRiskAssessment.risk)
    }

    @Test
    fun `returns HIGH risk when today is first day of fertile window and intercourse occurred that day`() {
        // Arrange
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 9)
        val intercourseEvents = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 9),
                protected = false
            )
        )


        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseEvents
        )

        // Assert
        assertEquals(PregnancyRisk.HIGH, assessment.risk)
    }

    @Test
    fun `returns HIGH risk when today is last day of fertile window and intercourse occurred earlier in window`() {
        // Arrange
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 14)
        val intercourseEvents = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = false
            )
        )

        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseEvents
        )

        // Assert
        assertEquals(PregnancyRisk.HIGH, assessment.risk)
    }

    @Test
    fun `returns MEDIUM risk when today is on fertile boundary but no intercourse occurred`() {
        // Arrange
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 14)

        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = emptyList()
        )

        // Assert
        assertEquals(PregnancyRisk.MEDIUM, assessment.risk)
    }

    @Test
    fun `returns LOW risk when intercourse occurred only outside fertile window`() {
        // Arrange
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 12)
        val intercourseEvents = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 5), // outside window
                protected = false
            )
        )


        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseEvents
        )

        // Assert
        assertEquals(PregnancyRisk.MEDIUM, assessment.risk)
    }

    @Test
    fun `returns LOW risk when today is outside fertile window even if intercourse occurred during fertile window`() {
        // Arrange
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 16)
        val intercourseEvents = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = false
            )
        )


        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseEvents
        )

        // Assert
        assertEquals(PregnancyRisk.LOW, assessment.risk)
    }

    @Test
    fun `returns HIGH risk when multiple intercourse days include at least one fertile day`() {
        // Arrange
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 11)
        val intercourseEvents = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = false
            )
        )

        // Act
        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = intercourseEvents
        )

        // Assert
        assertEquals(PregnancyRisk.HIGH, assessment.risk)
    }

    @Test
    fun `pregnancy risk decision table`() {
        val calculator = CycleCalculator()

        assertEquals(
            PregnancyRisk.HIGH,
            cycleCalculator.pregnancyRiskDecision(true, true)
        )

        assertEquals(
            PregnancyRisk.MEDIUM,
            cycleCalculator.pregnancyRiskDecision(true, false)
        )

        assertEquals(
            PregnancyRisk.LOW,
            cycleCalculator.pregnancyRiskDecision(false, true)
        )

        assertEquals(
            PregnancyRisk.LOW,
            cycleCalculator.pregnancyRiskDecision(false, false)
        )
    }

    @Test
    fun `pregnancyRiskDecision returns HIGH when fertile window and unprotected intercourse`() {
        val result = cycleCalculator.pregnancyRiskDecision(
            inFertileWindow = true,
            unprotectedIntercourseInWindow = true
        )

        assertEquals(PregnancyRisk.HIGH, result)
    }

    @Test
    fun `pregnancyRiskDecision returns MEDIUM when fertile window and no unprotected intercourse`() {
        val result = cycleCalculator.pregnancyRiskDecision(
            inFertileWindow = true,
            unprotectedIntercourseInWindow = false
        )

        assertEquals(PregnancyRisk.MEDIUM, result)
    }

    @Test
    fun `pregnancyRiskDecision returns LOW when not in fertile window but unprotected intercourse occurred`() {
        val result = cycleCalculator.pregnancyRiskDecision(
            inFertileWindow = false,
            unprotectedIntercourseInWindow = true
        )

        assertEquals(PregnancyRisk.LOW, result)
    }

    @Test
    fun `pregnancyRiskDecision returns LOW when not in fertile window and no intercourse`() {
        val result = cycleCalculator.pregnancyRiskDecision(
            inFertileWindow = false,
            unprotectedIntercourseInWindow = false
        )

        assertEquals(PregnancyRisk.LOW, result)
    }

    @Test
    fun `returns MEDIUM risk when intercourse occurred but all were protected`() {
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 12)

        val events = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = true
            )
        )

        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = events
        )

        assertEquals(PregnancyRisk.MEDIUM, assessment.risk)
    }

    @Test
    fun `returns HIGH risk when any unprotected intercourse occurred in fertile window`() {
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 12)

        val events = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 10),
                protected = true
            ),
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = false
            )
        )

        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = events
        )

        assertEquals(PregnancyRisk.HIGH, assessment.risk)
    }
    @Test
    fun `returns LOW risk when unprotected intercourse occurred outside fertile window`() {
        val fertileWindow = LocalDate.of(2025, 1, 9) to LocalDate.of(2025, 1, 14)
        val today = LocalDate.of(2025, 1, 20)

        val events = listOf(
            IntercourseEvent(
                date = LocalDate.of(2025, 1, 11),
                protected = false
            )
        )

        val assessment = cycleCalculator.calculatePregnancyRisk(
            today = today,
            fertileWindow = fertileWindow,
            intercourseEvents = events
        )

        assertEquals(PregnancyRisk.LOW, assessment.risk)
    }
}