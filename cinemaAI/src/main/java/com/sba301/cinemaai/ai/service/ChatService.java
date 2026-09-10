package com.sba301.cinemaai.ai.service;

import com.sba301.cinemaai.ai.dto.request.ChatRequest;
import com.sba301.cinemaai.ai.dto.response.ChatResponse;

public interface ChatService {
    ChatResponse chat(ChatRequest request);
}
