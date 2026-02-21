package org.tracker.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.tracker.agent.factory.CyclePredictionAgentFactory
import org.tracker.api.dto.ConfirmActualPeriodRequest
import org.tracker.api.dto.PredictionResponse
import org.tracker.api.dto.RecordPeriodRequest
import org.tracker.domain.CycleEntry
import java.util.*

@RestController
@RequestMapping("/api/agent")
class AgentController(
    private val cyclePredictionAgentFactory: CyclePredictionAgentFactory
) {

    private fun agentFor(userId: UUID) = cyclePredictionAgentFactory.forUser(userId)


    @PostMapping("/period")
    fun recordPeriod(
        @RequestBody request: RecordPeriodRequest
    ): ResponseEntity<Void> {
        val entry = CycleEntry(
            periodStart = request.periodStart,
            periodDuration = request.periodDuration,
            flowIntensity = request.flowIntensity,
            cervicalMucus = request.cervicalMucus,
            stressLevel = request.stressLevel,
            isIll = request.isIll,
            notes = request.notes
        )

        agentFor(request.userId).recordPeriod(entry)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/prediction")
    fun getPrediction(
        @RequestParam userId: UUID
    ): ResponseEntity<PredictionResponse> {
        val prediction = agentFor(userId).predictNextCycle()
        return ResponseEntity.ok(PredictionResponse.from(prediction))
    }

    @PostMapping("/actual")
    fun confirmActual(
        @RequestBody request: ConfirmActualPeriodRequest
    ): ResponseEntity<Void> {
        agentFor(request.userId).updateWithActualPeriodStartDate(request.actualStartDate)
        return ResponseEntity.ok().build()
    }

}