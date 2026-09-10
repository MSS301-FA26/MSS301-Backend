package com.sba301.cinemaai.ai.controller;

import com.sba301.cinemaai.ai.dto.request.ChatRequest;
import com.sba301.cinemaai.ai.dto.response.ChatResponse;
import com.sba301.cinemaai.ai.service.ChatService;
import com.sba301.cinemaai.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.chat(request));
    }
}
