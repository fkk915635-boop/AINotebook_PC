package com.ainotebook.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogDetailItem {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String summary;
    private Integer liked;
    private LocalDateTime createdAt;

    private String authorNickname;
    private String authorAvatar;
    private boolean mine;
    private boolean followed;
    private boolean likedByMe;
}

