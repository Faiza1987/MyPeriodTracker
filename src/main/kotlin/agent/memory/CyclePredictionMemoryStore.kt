package org.tracker.agent.memory

import org.tracker.agent.CyclePredictionMemory
import java.util.UUID

interface CyclePredictionMemoryStore {
    fun load(userId: UUID): CyclePredictionMemory
    fun save(userId: UUID, memory: CyclePredictionMemory)
}


