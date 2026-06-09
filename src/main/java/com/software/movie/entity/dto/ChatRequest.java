package com.software.movie.entity.dto;

import lombok.Data;

/**
 * AI 聊天请求体。
 */
@Data
public class ChatRequest {
    /** 用户输入的消息 */
    private String message;
}
