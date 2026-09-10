package com.sba301.cinemaai.ai.service.impl;

import com.sba301.cinemaai.ai.client.AIClient;
import com.sba301.cinemaai.ai.dto.request.ChatRequest;
import com.sba301.cinemaai.ai.dto.response.AIChatResult;
import com.sba301.cinemaai.ai.dto.response.ChatResponse;
import com.sba301.cinemaai.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final AIClient aiClient;

    @Override
    public ChatResponse chat(ChatRequest request) {
        AIChatResult result = aiClient.chat(request);
        return new ChatResponse(result.getMessage(), result.getConversationId(), result.getData());
    }

}
