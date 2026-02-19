package org.tracker.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.*
import org.tracker.domain.*
import org.tracker.repository.PredictionRepository
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class AgenticCycleServiceTest {

    private val chatClient: ChatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
    private val predictionRepository: PredictionRepository = mock(PredictionRepository::class.java)
    private val agenticCycleService = AgenticCycleService(chatClient, predictionRepository)

    @Test
    fun `should predict and call repository save`() {
        // Arrange
        val userId = UUID.randomUUID()
        val profile = UserProfile(averageCycleLength = 28, cycles = emptyList())
        val mockPrediction = CyclePrediction(
            predictedStartDate = LocalDate.now().plusDays(28),
            explanation = PredictionExplanation(
                confidence = PredictionConfidence.MEDIUM,
                reasons = listOf("Based on avg")
            )
        )

        `when`(chatClient.prompt()
            .user(anyString())
            .call()
            .entity(CyclePrediction::class.java)
        ).thenReturn(mockPrediction)

        // Act
        val result = agenticCycleService.predictAndSave(userId, profile)

        // Assert
        assertEquals(mockPrediction.predictedStartDate, result.predictedStartDate)
        assertEquals(mockPrediction.explanation.confidence, result.explanation.confidence)

        // Verify the repository was called exactly once with the AI's result
        verify(predictionRepository, times(1)).savePrediction(userId, mockPrediction)
    }
}