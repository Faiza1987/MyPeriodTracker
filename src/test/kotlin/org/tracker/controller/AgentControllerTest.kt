package org.tracker.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.tracker.agent.CyclePredictionAgent
import org.tracker.agent.CyclePredictionMemory
import org.tracker.agent.CyclePredictionMemoryStore
import org.tracker.agent.InMemoryCyclePredictionMemoryStore
import org.tracker.agent.factory.CyclePredictionAgentFactory
import org.tracker.api.dto.ConfirmActualPeriodRequest
import org.tracker.api.dto.RecordPeriodRequest
import org.tracker.domain.Cycle
import org.tracker.domain.CyclePrediction
import org.tracker.domain.PredictionConfidence
import org.tracker.domain.PredictionExplanation
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(AgentController::class)
class AgentControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var cyclePredictionAgentFactory: CyclePredictionAgentFactory


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