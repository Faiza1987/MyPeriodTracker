package org.tracker.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.tracker.agent.CyclePredictionAgent
import org.tracker.agent.CyclePredictionMemory
import org.tracker.agent.InMemoryCyclePredictionMemoryStore
import org.tracker.agent.factory.CyclePredictionAgentFactory
import org.tracker.api.dto.AIPredictionRequest
import org.tracker.api.dto.ConfirmActualPeriodRequest
import org.tracker.api.dto.RecordPeriodRequest
import org.tracker.domain.Cycle
import org.tracker.domain.CycleEntry
import org.tracker.domain.CyclePrediction
import org.tracker.domain.PredictionConfidence
import org.tracker.domain.PredictionExplanation
import org.tracker.domain.UserProfile
import org.tracker.exceptions.GlobalExceptionHandler
import org.tracker.service.AgenticCycleService
import java.time.LocalDate
import java.util.UUID

@Import(GlobalExceptionHandler::class)
@WebMvcTest(AgentController::class)
class AgentControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var cyclePredictionAgentFactory: CyclePredictionAgentFactory

    @MockBean
    lateinit var agenticCycleService: AgenticCycleService


    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private fun agentWithCycles(cycles: List<Cycle> = emptyList()): CyclePredictionAgent {
        val store = InMemoryCyclePredictionMemoryStore()
        store.save(userId, CyclePredictionMemory(cycles = cycles))
        return CyclePredictionAgent(userId, store)
    }

    private fun stubFactory(agent: CyclePredictionAgent) {
        whenever(cyclePredictionAgentFactory.forUser(any())).thenReturn(agent)
    }

    @Test
    fun `POST period records the period and returns 200`() {
        stubFactory(agentWithCycles())

        mockMvc.post("/api/agent/period") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RecordPeriodRequest(
                    userId = userId,
                    periodStart = LocalDate.of(2025, 1, 1),
                    stressLevel = 3,
                    isIll = false
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `POST period with symptom data returns 200`() {
        stubFactory(agentWithCycles())

        mockMvc.post("/api/agent/period") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                RecordPeriodRequest(
                    userId = userId,
                    periodStart = LocalDate.of(2025, 1, 1),
                    flowIntensity = "HEAVY",
                    cervicalMucus = "EGG_WHITE",
                    stressLevel = 5,
                    isIll = true,
                    notes = "Felt unwell"
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

    // --- GET /api/agent/prediction ---

    @Test
    fun `GET prediction returns predicted date, confidence and reasons`() {
        stubFactory(agentWithCycles(listOf(
            Cycle(startDate = LocalDate.of(2025, 1, 1)),
            Cycle(startDate = LocalDate.of(2025, 1, 29)),
            Cycle(startDate = LocalDate.of(2025, 2, 26))
        )))

        mockMvc.get("/api/agent/prediction") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.predictedStartDate") { value("2025-03-26") }
            jsonPath("$.confidence") { value("HIGH") }
            jsonPath("$.reasons") { isArray() }
        }
    }

    @Test
    fun `GET prediction downgrades confidence when high stress is logged`() {
        stubFactory(agentWithCycles(listOf(
            Cycle(startDate = LocalDate.of(2025, 1, 1)),
            Cycle(startDate = LocalDate.of(2025, 1, 29)),
            Cycle(startDate = LocalDate.of(2025, 2, 26), stressLevel = 5)
        )))

        mockMvc.get("/api/agent/prediction") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.confidence") { value(PredictionConfidence.MEDIUM.name) }
        }
    }

    // --- POST /api/agent/ai-prediction ---
    @Test
    fun `POST ai-prediction returns AI-refined prediction with statistical baseline`() {
        // Agent has enough cycles for a statistical baseline
        stubFactory(agentWithCycles(listOf(
            Cycle(startDate = LocalDate.of(2025, 1, 1)),
            Cycle(startDate = LocalDate.of(2025, 1, 29)),
            Cycle(startDate = LocalDate.of(2025, 2, 26))
        )))

        val aiPrediction = CyclePrediction(
            predictedStartDate = LocalDate.of(2025, 3, 28), // AI adjusted by 2 days
            explanation = PredictionExplanation(
                confidence = PredictionConfidence.HIGH,
                reasons = listOf("Baseline confirmed", "No disruptors detected")
            )
        )
        whenever(agenticCycleService.predictAndSave(any(), any(), anyOrNull())).thenReturn(aiPrediction)

        val request = AIPredictionRequest(
            userId = userId,
            userProfile = UserProfile(
                averageCycleLength = 28,
                cycles = listOf(
                    CycleEntry(periodStart = LocalDate.of(2025, 1, 1)),
                    CycleEntry(periodStart = LocalDate.of(2025, 1, 29)),
                    CycleEntry(periodStart = LocalDate.of(2025, 2, 26))
                )
            )
        )

        mockMvc.post("/api/agent/ai-prediction") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.predictedStartDate") { value("2025-03-28") }
            jsonPath("$.confidence") { value("HIGH") }
            jsonPath("$.reasons") { isArray() }
        }
    }

    @Test
    fun `POST ai-prediction with fewer than 2 cycles returns 400`() {
        // Agent has no cycles — throws InsufficientCycleHistoryException → 400
        stubFactory(agentWithCycles())

        val request = AIPredictionRequest(
            userId = userId,
            userProfile = UserProfile(averageCycleLength = 28, cycles = emptyList())
        )

        mockMvc.post("/api/agent/ai-prediction") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value("Not enough cycle history to make a prediction. Please record at least 2 periods.") }
        }
    }

    // --- GET /api/agent/cycles ---

    @Test
    fun `GET cycles returns all cycles sorted most recent first`() {
        stubFactory(agentWithCycles(listOf(
            Cycle(startDate = LocalDate.of(2025, 1, 1)),
            Cycle(startDate = LocalDate.of(2025, 1, 29)),
            Cycle(startDate = LocalDate.of(2025, 2, 26))
        )))

        mockMvc.get("/api/agent/cycles") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalCycles") { value(3) }
            jsonPath("$.cycles[0].startDate") { value("2025-02-26") }
            jsonPath("$.cycles[1].startDate") { value("2025-01-29") }
            jsonPath("$.cycles[2].startDate") { value("2025-01-01") }
            jsonPath("$.stats.averageCycleLength") { value(28) }
            jsonPath("$.stats.shortestCycle") { value(28) }
            jsonPath("$.stats.longestCycle") { value(28) }
        }
    }

    @Test
    fun `GET cycles returns empty list and null stats when no cycles recorded`() {
        stubFactory(agentWithCycles())

        mockMvc.get("/api/agent/cycles") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalCycles") { value(0) }
            jsonPath("$.cycles") { isArray() }
            jsonPath("$.stats") { doesNotExist() }
        }
    }

    @Test
    fun `GET cycles includes symptom data`() {
        stubFactory(agentWithCycles(listOf(
            Cycle(
                startDate = LocalDate.of(2025, 1, 1),
                stressLevel = 4,
                isIll = true,
                flowIntensity = "HEAVY",
                cervicalMucus = "EGG_WHITE",
                notes = "Felt rough"
            )
        )))

        mockMvc.get("/api/agent/cycles") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.cycles[0].stressLevel") { value(4) }
            jsonPath("$.cycles[0].isIll") { value(true) }
            jsonPath("$.cycles[0].flowIntensity") { value("HEAVY") }
            jsonPath("$.cycles[0].cervicalMucus") { value("EGG_WHITE") }
            jsonPath("$.cycles[0].notes") { value("Felt rough") }
        }
    }

    @Test
    fun `GET cycles with only 1 cycle returns no stats`() {
        stubFactory(agentWithCycles(listOf(
            Cycle(startDate = LocalDate.of(2025, 1, 1))
        )))

        mockMvc.get("/api/agent/cycles") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalCycles") { value(1) }
            jsonPath("$.stats") { doesNotExist() }
        }
    }


    // --- POST /api/agent/actual ---

    @Test
    fun `POST actual confirms actual period date and returns 200`() {
        // Agent needs a lastPrediction set so updateWithActual has something to record
        val store = InMemoryCyclePredictionMemoryStore()
        val agent = CyclePredictionAgent(userId, store)
        agent.recordPeriod(LocalDate.of(2025, 1, 1))
        agent.recordPeriod(LocalDate.of(2025, 1, 29))
        agent.predictNextCycle()
        stubFactory(agent)

        mockMvc.post("/api/agent/actual") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ConfirmActualPeriodRequest(
                    userId = userId,
                    actualStartDate = LocalDate.of(2025, 3, 28)
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `POST actual with no prior prediction still returns 200`() {
        stubFactory(agentWithCycles())

        mockMvc.post("/api/agent/actual") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ConfirmActualPeriodRequest(
                    userId = userId,
                    actualStartDate = LocalDate.of(2025, 3, 28)
                )
            )
        }.andExpect {
            status { isOk() }
        }
    }
}