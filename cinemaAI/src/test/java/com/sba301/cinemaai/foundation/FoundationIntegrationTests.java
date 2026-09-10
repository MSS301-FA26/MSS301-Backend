package com.sba301.cinemaai.foundation;

import com.sba301.cinemaai.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FoundationIntegrationTests.TestFoundationController.class)
class FoundationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldEchoProvidedCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/auth/test-foundation/ping")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "phase-0-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "phase-0-correlation-id"))
                .andExpect(content().string("pong"));
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldExposeOpenApiDocsWithBearerSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andExpect(jsonPath("$.info.title").value("CineAI API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].bearerFormat").value("JWT"));
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().is4xxClientError())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void shouldReturnStandardErrorResponseForMissingResource() throws Exception {
        mockMvc.perform(get("/api/v1/auth/phase-0-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy tài nguyên"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/phase-0-not-found"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    @RequestMapping("/api/v1/auth/test-foundation")
    static class TestFoundationController {

        @GetMapping("/ping")
        String ping() {
            return "pong";
        }
    }
}
