package org.tracker.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.tracker.agent.factory.CyclePredictionAgentFactory
import org.tracker.api.dto.AIPredictionRequest
import org.tracker.api.dto.ConfirmActualPeriodRequest
import org.tracker.api.dto.CycleHistoryResponse
import org.tracker.api.dto.PredictionResponse
import org.tracker.api.dto.RecordPeriodRequest
import org.tracker.domain.CycleEntry
import org.tracker.service.AgenticCycleService
import java.util.*

@RestController
@RequestMapping("/api/agent")
class AgentController(
    private val cyclePredictionAgentFactory: CyclePredictionAgentFactory,
    private val agenticCycleService: AgenticCycleService
) {

    private fun agentFor(userId: UUID) = cyclePredictionAgentFactory.forUser(userId)

    @GetMapping("/cycles")
    fun getCycleHistory(
        @RequestParam userId: UUID
    ): ResponseEntity<CycleHistoryResponse> {
        val cycles = agentFor(userId).getCycles()
        return ResponseEntity.ok(CycleHistoryResponse.from(cycles))
    }


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

    @PostMapping("/ai-prediction")
    fun getAIPrediction(
        @RequestBody request: AIPredictionRequest
    ) : ResponseEntity<PredictionResponse> {
        // Step 1: get the statistical baseline — throws InsufficientCycleHistoryException
        // if fewer than 2 cycles exist, which GlobalExceptionHandler maps to a 400
        val statisticalBaseline = agentFor(request.userId).predictNextCycle()

        // Step 2: pass baseline + full cycle history to the AI for refinement
        val aiPrediction = agenticCycleService.predictAndSave(
            request.userId,
            request.userProfile,
            statisticalBaseline = statisticalBaseline
        )
        return ResponseEntity.ok(PredictionResponse.from(aiPrediction))
    }

    @PostMapping("/actual")
    fun confirmActual(
        @RequestBody request: ConfirmActualPeriodRequest
    ): ResponseEntity<Void> {
        agentFor(request.userId).updateWithActualPeriodStartDate(request.actualStartDate)
        return ResponseEntity.ok().build()
    }

}