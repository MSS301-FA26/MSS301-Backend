package com.sba301.cinemaai.ai.dto.request;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private Long movieId;
    private Long userId;
    private String conversationId;
}
