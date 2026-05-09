package com.ainotebook.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_sessions")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String assistantName;
    private String systemPrompt;
    private String model;
    private String baseUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

