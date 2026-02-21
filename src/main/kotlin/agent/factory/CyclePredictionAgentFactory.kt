package org.tracker.agent.factory

import org.springframework.stereotype.Component
import org.tracker.agent.CyclePredictionAgent
import org.tracker.agent.CyclePredictionMemoryStore
import java.util.UUID

@Component
class CyclePredictionAgentFactory(
    private val memoryStore: CyclePredictionMemoryStore
) {
    fun forUser(userId: UUID): CyclePredictionAgent =
        CyclePredictionAgent(userId, memoryStore)
}