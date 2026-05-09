package com.ainotebook.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentItem {
    private Long id;
    private Long blogId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    private String authorNickname;
    private String authorAvatar;
    private boolean mine;
}

