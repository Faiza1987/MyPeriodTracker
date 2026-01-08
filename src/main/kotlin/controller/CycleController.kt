package org.tracker.controller

import org.tracker.service.CycleCalculator
import org.springframework.web.bind.annotation.*
import org.tracker.api.dto.CycleSummaryRequest
import org.tracker.api.dto.CycleSummaryResponse

@RestController
@RequestMapping("/api/cycle")
class CycleController(
    private val cycleCalculator: CycleCalculator
) {

    @PostMapping("/summary")
    fun generateSummary(
        @RequestBody request: CycleSummaryRequest
    ): CycleSummaryResponse {

        val summary = cycleCalculator.generateCycleSummary(
            today = request.today,
            user = request.user
        )

        return CycleSummaryResponse.from(summary)
    }
}