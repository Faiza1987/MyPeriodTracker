package org.tracker.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.stereotype.Service
import org.tracker.domain.CyclePrediction
import org.tracker.domain.UserProfile
import org.tracker.repository.PredictionRepository
import java.util.UUID

@Service
class AgenticCycleService(
    private val chatClient: ChatClient,
    private val predictionRepository: PredictionRepository,
) {

    fun predictAndSave(userId: UUID, userProfile: UserProfile, statisticalBaseline: CyclePrediction? = null,): CyclePrediction {
        // prepare data for the LLM
        val historyContext = buildHistoryContext(userProfile)
        val baselineContext = buildBaselineContext(statisticalBaseline)
        val prompt = buildPrompt(historyContext, baselineContext)

        // the AI reasoning loop
        val prediction = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(CyclePrediction::class.java)
            ?: throw IllegalStateException("AI failed to generate a valid prediction")

        // Persistence of the prediction (AI Agent's memory)
        predictionRepository.savePrediction(userId, prediction)

        return prediction
    }

    // The OutputConverter ensures the AI returns JSON matching your CyclePrediction class
    private val outputConverter = BeanOutputConverter(CyclePrediction::class.java)

    fun predictWithContext(userProfile: UserProfile): CyclePrediction {
        val historyContext = userProfile.cycles.joinToString("\n") {
            "Date: ${it.periodStart}, Stress: ${it.stressLevel}, Mucus: ${it.cervicalMucus}, Flow: ${it.flowIntensity}, Ill: ${it.isIll}, Notes: ${it.notes}"
        }

        return chatClient.prompt()
            .user("""
                Here is my cycle history:
                $historyContext
                
                Based on this information, predict my next period start date. 
                Consider stress levels and illness as potential delay factors.
            """.trimIndent())
            // This is the magic line that tells the AI to return JSON
            .call()
            .entity(CyclePrediction::class.java)
            ?: throw IllegalStateException("AI failed to generate a valid prediction")
    }

    private fun buildHistoryContext(userProfile: UserProfile) : String {
        if(userProfile.cycles.isEmpty()) return "No cycle history available."

        return userProfile.cycles.joinToString("\n") {
            "Date: ${it.periodStart}, " +
                    "Stress: ${it.stressLevel ?: "not logged"}, " +
                    "Mucus: ${it.cervicalMucus ?: "not logged"}, " +
                    "Flow: ${it.flowIntensity ?: "not logged"}, " +
                    "Ill: ${it.isIll}, " +
                    "Notes: ${it.notes ?: "none"}"
        }
    }

    private fun buildBaselineContext(baseline: CyclePrediction?) : String {
        if(baseline == null) return "No statistical baseline available."

        val reasons = baseline.explanation.reasons.joinToString("; ")

        return """
            Statistical model prediction:
            - Predicted start date: ${baseline.predictedStartDate}
            - Confidence: ${baseline.explanation.confidence.name}
            - Reasons: $reasons
        """.trimIndent()
    }

    private fun buildPrompt(historyContext: String, baselineContext: String): String = """
        You are analysing menstrual cycle data to predict the next period start date.
        
        STATISTICAL BASELINE:
        $baselineContext
        
        CYCLE HISTORY:
        $historyContext
        
        Using the statistical baseline as your starting point, refine the prediction 
        if the cycle history justifies it. In particular:
        - If stress level 4 or 5 was logged in the most recent cycle, consider a delay of 1-3 days
        - If illness was logged in the most recent cycle, consider a delay of 1-5 days
        - If both stress and illness were logged, consider a delay of up to 7 days
        - If the cycle history is consistent with the baseline, confirm it
        
        Explain your reasoning clearly in the response.
    """.trimIndent()
}