package org.tracker.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.stereotype.Service
import org.tracker.domain.CyclePrediction
import org.tracker.domain.UserProfile

@Service
class AgenticCycleService(private val chatClient: ChatClient) {

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
}