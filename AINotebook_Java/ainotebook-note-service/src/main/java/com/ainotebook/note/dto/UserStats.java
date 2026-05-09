package com.ainotebook.note.dto;

import lombok.Data;

@Data
public class UserStats {
    private Integer totalLikes;
    private Integer followerCount;
    private Integer followingCount;
}

