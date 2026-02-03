package org.tracker.agent

class InMemoryCyclePredictionMemoryStore(
    initialMemory: CyclePredictionMemory = CyclePredictionMemory()
) : CyclePredictionMemoryStore {

    private var memory: CyclePredictionMemory = initialMemory

    override fun load(): CyclePredictionMemory = memory

    override fun save(memory: CyclePredictionMemory) {
        this.memory = memory
    }
}
