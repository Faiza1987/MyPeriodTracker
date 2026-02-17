package org.tracker.agent.memory

import org.tracker.agent.CyclePredictionResult
import java.util.UUID

interface PredictionMemoryStore {
    fun savePredictionResult(userId: UUID, result: CyclePredictionResult)
    fun getRecentPredictionResults(userId: UUID, limit: Int = 3) : List<CyclePredictionResult>
}