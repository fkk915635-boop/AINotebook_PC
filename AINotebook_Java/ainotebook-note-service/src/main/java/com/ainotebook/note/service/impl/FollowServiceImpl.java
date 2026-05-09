package com.ainotebook.note.service.impl;

import com.ainotebook.note.service.FollowService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl implements FollowService {

    private final StringRedisTemplate stringRedisTemplate;

    public FollowServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private String followingsKey(Long userId) {
        return "user:followings:" + userId;
    }

    private String followersKey(Long userId) {
        return "user:followers:" + userId;
    }

    @Override
    public boolean toggleFollow(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) throw new IllegalArgumentException("参数不能为空");
        if (userId.equals(targetUserId)) throw new IllegalArgumentException("不能关注自己");

        String followingKey = followingsKey(userId);
        String followerKey = followersKey(targetUserId);
        String target = String.valueOf(targetUserId);
        String me = String.valueOf(userId);

        Boolean isMember = stringRedisTemplate.opsForSet().isMember(followingKey, target);
        if (Boolean.TRUE.equals(isMember)) {
            stringRedisTemplate.opsForSet().remove(followingKey, target);
            stringRedisTemplate.opsForSet().remove(followerKey, me);
            return false;
        }
        stringRedisTemplate.opsForSet().add(followingKey, target);
        stringRedisTemplate.opsForSet().add(followerKey, me);
        return true;
    }

    @Override
    public boolean isFollowed(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) return false;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(followingsKey(userId), String.valueOf(targetUserId));
        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public int followerCount(Long userId) {
        Long size = stringRedisTemplate.opsForSet().size(followersKey(userId));
        return size == null ? 0 : size.intValue();
    }

    @Override
    public int followingCount(Long userId) {
        Long size = stringRedisTemplate.opsForSet().size(followingsKey(userId));
        return size == null ? 0 : size.intValue();
    }
}

