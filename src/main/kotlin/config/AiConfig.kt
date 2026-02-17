package org.tracker.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient {
        // This creates a reusable client with a baseline "Expert" persona
        return builder
            .defaultSystem("""
                You are a specialized health AI agent for 'MyPeriodTracker'.
                Your primary goal is to analyze menstrual cycle history and lifestyle data 
                (stress, illness, symptoms) to provide accurate predictions and insights.
                Always be compassionate, scientific, and clear.
            """.trimIndent())
            .build()
    }
}