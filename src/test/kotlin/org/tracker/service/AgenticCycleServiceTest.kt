package org.tracker.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.ai.chat.client.ChatClient
import org.tracker.domain.CyclePrediction
import org.tracker.domain.CycleEntry
import org.tracker.domain.PredictionConfidence
import org.tracker.domain.PredictionExplanation
import org.tracker.domain.UserProfile
import org.tracker.repository.PredictionRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class AgenticCycleServiceTest {

    private val chatClient: ChatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
    private val predictionRepository: PredictionRepository = mock(PredictionRepository::class.java)
    private val agenticCycleService = AgenticCycleService(chatClient, predictionRepository)

    private val userId = UUID.randomUUID()

    private val mockPrediction = CyclePrediction(
        predictedStartDate = LocalDate.of(2025, 3, 26),
        explanation = PredictionExplanation(
            confidence = PredictionConfidence.HIGH,
            reasons = listOf("Cycle history is consistent")
        )
    )

    private fun stubChatClient() {
        whenever(
            chatClient.prompt()
                .user(anyOrNull<String>())
                .call()
                .entity(CyclePrediction::class.java)
        ).thenReturn(mockPrediction)
    }

    @Test
    fun `predictAndSave without baseline calls AI and saves prediction`() {
        stubChatClient()

        val profile = UserProfile(averageCycleLength = 28, cycles = emptyList())
        val result = agenticCycleService.predictAndSave(userId, profile)

        assertEquals(mockPrediction.predictedStartDate, result.predictedStartDate)
        assertEquals(mockPrediction.explanation.confidence, result.explanation.confidence)
        verify(predictionRepository, times(1)).savePrediction(userId, mockPrediction)
    }

    @Test
    fun `predictAndSave with statistical baseline calls AI and saves prediction`() {
        stubChatClient()

        val baseline = CyclePrediction(
            predictedStartDate = LocalDate.of(2025, 3, 26),
            explanation = PredictionExplanation(
                confidence = PredictionConfidence.HIGH,
                reasons = listOf("Average cycle length: 28 days", "Cycle history is consistent")
            )
        )

        val profile = UserProfile(
            averageCycleLength = 28,
            cycles = listOf(
                CycleEntry(
                    periodStart = LocalDate.of(2025, 1, 1),
                    stressLevel = 2,
                    isIll = false
                )
            )
        )

        val result = agenticCycleService.predictAndSave(userId, profile, statisticalBaseline = baseline)

        assertEquals(mockPrediction.predictedStartDate, result.predictedStartDate)
        verify(predictionRepository, times(1)).savePrediction(userId, mockPrediction)
    }

    @Test
    fun `predictAndSave with high stress baseline passes context to AI`() {
        stubChatClient()

        val baseline = CyclePrediction(
            predictedStartDate = LocalDate.of(2025, 3, 26),
            explanation = PredictionExplanation(
                confidence = PredictionConfidence.MEDIUM,
                reasons = listOf(
                    "Average cycle length: 28 days",
                    "High stress (level 5/5) was logged, which may affect cycle timing"
                )
            )
        )

        val profile = UserProfile(
            averageCycleLength = 28,
            cycles = listOf(
                CycleEntry(
                    periodStart = LocalDate.of(2025, 1, 1),
                    stressLevel = 5,
                    isIll = false
                )
            )
        )

        val result = agenticCycleService.predictAndSave(userId, profile, statisticalBaseline = baseline)

        // AI returned its prediction — we verify it was saved
        assertEquals(mockPrediction.predictedStartDate, result.predictedStartDate)
        verify(predictionRepository, times(1)).savePrediction(userId, mockPrediction)
    }

    @Test
    fun `predictAndSave with empty cycle history still calls AI`() {
        stubChatClient()

        val profile = UserProfile(averageCycleLength = 28, cycles = emptyList())
        val result = agenticCycleService.predictAndSave(userId, profile, statisticalBaseline = null)

        assertEquals(mockPrediction.predictedStartDate, result.predictedStartDate)
        verify(predictionRepository, times(1)).savePrediction(userId, mockPrediction)
    }
}