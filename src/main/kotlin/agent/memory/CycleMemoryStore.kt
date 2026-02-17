package org.tracker.agent.memory

import org.tracker.domain.Cycle
import java.util.UUID

interface CycleMemoryStore {
    fun recordCycle(userId: UUID, cycle: Cycle)
    fun getCyclesForUser(userId: UUID) : List<Cycle>
}