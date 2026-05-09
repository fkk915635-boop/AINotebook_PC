package com.ainotebook.note.controller;

import com.ainotebook.common.security.JwtUtil;
import com.ainotebook.note.service.FollowService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/follow")
public class FollowController {

    private final FollowService followService;

    @Value("${ainotebook.jwt.secret:ainotebook-dev-secret}")
    private String secret;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/toggle/{targetId}")
    public Map<String, Object> toggle(@PathVariable Long targetId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        boolean followed = followService.toggleFollow(userId, targetId);
        return Map.of("followed", followed);
    }

    @GetMapping("/status/{targetId}")
    public Map<String, Object> status(@PathVariable Long targetId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        boolean followed = followService.isFollowed(userId, targetId);
        return Map.of("followed", followed);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = JwtUtil.parseClaims(token, secret);
        return Long.parseLong(claims.getSubject());
    }
}

