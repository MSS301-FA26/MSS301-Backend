package com.sba301.cinemaai.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTests.TestExceptionController.class)
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnStandardErrorResponseForBusinessException() throws Exception {
        mockMvc.perform(post("/api/v1/auth/test-errors/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid test request"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/test-errors/bad-request"));
    }

    @Test
    void shouldReturnValidationErrorResponse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @RestController
    @RequestMapping("/api/v1/auth/test-errors")
    static class TestExceptionController {

        @PostMapping("/bad-request")
        void badRequest() {
            throw new BadRequestException("Invalid test request");
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody TestValidationRequest request) {
        }
    }

    record TestValidationRequest(
            @NotBlank(message = "Name is required")
            String name
    ) {
    }
}
