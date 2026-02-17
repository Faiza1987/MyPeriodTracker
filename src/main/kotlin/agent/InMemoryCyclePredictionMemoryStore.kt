package org.tracker.agent

import java.util.UUID

class InMemoryCyclePredictionMemoryStore(
    initialMemory: CyclePredictionMemory = CyclePredictionMemory()
) : CyclePredictionMemoryStore {

    private val memoryByUser = mutableMapOf<UUID, CyclePredictionMemory>()

    override fun load(userId: UUID): CyclePredictionMemory =
        memoryByUser[userId] ?: CyclePredictionMemory()

    override fun save(userId: UUID, memory: CyclePredictionMemory) {
        memoryByUser[userId] = memory
    }
}
