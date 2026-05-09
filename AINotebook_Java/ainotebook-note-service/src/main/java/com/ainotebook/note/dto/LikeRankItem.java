package com.ainotebook.note.dto;

import lombok.Data;

@Data
public class LikeRankItem {
    private Long id;
    private String title;
    private Integer liked;
    private Long userId;
    private String authorNickname;
    private String authorAvatar;
}

