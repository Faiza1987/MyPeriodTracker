package org.tracker.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.tracker.TestFixtures
import org.tracker.service.CycleCalculator
import java.time.LocalDate


@WebMvcTest(CycleController::class)
class CycleControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var cycleCalculator: CycleCalculator

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun postApiCycleSummaryReturnsCycleSummaryResponse() {
        // Arrange
        val today = LocalDate.of(2025, 1, 12)

        val request = TestFixtures.cycleSummaryRequest(
            today = today
        )

        val domainSummary = TestFixtures.cycleSummary()

        `when`(
            cycleCalculator.generateCycleSummary(
                today = today,
                user = request.user
            )
        ).thenReturn(domainSummary)

        // Act & Assert
        mockMvc.post("/api/cycle/summary") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.cycleDay") { value(12) }
                jsonPath("$.fertileWindowStart") { value("2025-01-09") }
                jsonPath("$.fertileWindowEnd") { value("2025-01-14") }
                jsonPath("$.pregnancyRisk") { value("HIGH") }
                jsonPath("$.reasons") { isArray() }
            }

        verify(cycleCalculator).generateCycleSummary(
            today = today,
            user = request.user
        )

    }
}