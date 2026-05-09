package com.ainotebook.note.controller;

import com.ainotebook.common.security.JwtUtil;
import com.ainotebook.note.dto.UserStats;
import com.ainotebook.note.mapper.BlogMapper;
import com.ainotebook.note.service.FollowService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final BlogMapper blogMapper;
    private final FollowService followService;

    @Value("${ainotebook.jwt.secret:ainotebook-dev-secret}")
    private String secret;

    public UserController(BlogMapper blogMapper, FollowService followService) {
        this.blogMapper = blogMapper;
        this.followService = followService;
    }

    @GetMapping("/stats")
    public UserStats stats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromHeader(authHeader);
        UserStats stats = new UserStats();
        stats.setTotalLikes(blogMapper.sumLikesByUserId(userId));
        stats.setFollowerCount(followService.followerCount(userId));
        stats.setFollowingCount(followService.followingCount(userId));
        return stats;
    }

    private Long getUserIdFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = JwtUtil.parseClaims(token, secret);
            return Long.parseLong(claims.getSubject());
        }
        throw new IllegalArgumentException("未获取到有效的用户认证信息");
    }
}

