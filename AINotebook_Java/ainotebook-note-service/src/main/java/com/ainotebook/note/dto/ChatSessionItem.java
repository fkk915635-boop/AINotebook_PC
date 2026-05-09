package com.ainotebook.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionItem {
    private Long id;
    private String title;
    private String assistantName;
    private String systemPrompt;
    private String model;
    private String baseUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
