package org.tracker.agent

interface CyclePredictionMemoryStore {
    fun load(): CyclePredictionMemory
    fun save(memory: CyclePredictionMemory)
}