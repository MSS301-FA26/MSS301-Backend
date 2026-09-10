package com.sba301.cinemaai.ai.dto.response;

import lombok.Data;

import java.util.Map;

/** Ánh xạ trực tiếp response JSON từ FastAPI /chat: { message, conversationId, data } */
@Data
public class AIChatResult {
    private String message;
    private String conversationId;
    private Map<String, Object> data;
}
