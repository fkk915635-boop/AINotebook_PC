package com.ainotebook.note.service;

public interface FollowService {
    boolean toggleFollow(Long userId, Long targetUserId);
    boolean isFollowed(Long userId, Long targetUserId);
    int followerCount(Long userId);
    int followingCount(Long userId);
}

