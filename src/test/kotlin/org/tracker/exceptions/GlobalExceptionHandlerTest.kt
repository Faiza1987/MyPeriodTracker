package org.tracker.exceptions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.tracker.agent.CyclePredictionAgent
import org.tracker.agent.CyclePredictionMemory
import org.tracker.agent.InMemoryCyclePredictionMemoryStore
import org.tracker.agent.factory.CyclePredictionAgentFactory
import org.tracker.controller.AgentController
import org.tracker.service.AgenticCycleService
import java.util.UUID

@WebMvcTest(AgentController::class)
@Import(GlobalExceptionHandler::class)
class GlobalExceptionHandlerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var cyclePredictionAgentFactory: CyclePredictionAgentFactory

    @MockBean
    lateinit var agenticCycleService: AgenticCycleService

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `GET prediction with fewer than 2 cycles returns 400 with helpful message`() {
        // Agent has no cycles — predictNextCycle throws InsufficientCycleHistoryException
        val store = InMemoryCyclePredictionMemoryStore()
        store.save(userId, CyclePredictionMemory(cycles = emptyList()))
        val agent = CyclePredictionAgent(userId, store)
        whenever(cyclePredictionAgentFactory.forUser(any())).thenReturn(agent)

        mockMvc.get("/api/agent/prediction") {
            param("userId", userId.toString())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.error") { value("Bad Request") }
            jsonPath("$.message") { value("Not enough cycle history to make a prediction. Please record at least 2 periods.") }
        }
    }

    @Test
    fun `GET prediction with only 1 cycle returns 400`() {
        val store = InMemoryCyclePredictionMemoryStore()
        val agent = CyclePredictionAgent(userId, store)
        agent.recordPeriod(java.time.LocalDate.of(2025, 1, 1)) // only 1 cycle
        whenever(cyclePredictionAgentFactory.forUser(any())).thenReturn(agent)

        mockMvc.get("/api/agent/prediction") {
            param("userId", userId.toString())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value("Not enough cycle history to make a prediction. Please record at least 2 periods.") }
        }
    }
}