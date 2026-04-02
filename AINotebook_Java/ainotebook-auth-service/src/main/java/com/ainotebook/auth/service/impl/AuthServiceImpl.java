package com.ainotebook.auth.service.impl;

import com.ainotebook.auth.entity.User;
import com.ainotebook.auth.mapper.UserMapper;
import com.ainotebook.auth.service.AuthService;
import com.ainotebook.common.security.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${ainotebook.jwt.secret:ainotebook-dev-secret}")
    private String jwtSecret;

    @Value("${ainotebook.jwt.ttlMillis:604800000}")
    private long ttlMillis;

    public AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void register(String username, String password) {
        String u = normalize(username);
        if (u.isEmpty() || password == null || password.length() < 6) {
            throw new IllegalArgumentException("账号或密码不合法");
        }
        User exists = findByUsername(u);
        if (exists != null) {
            throw new IllegalArgumentException("账号已存在");
        }
        User user = new User();
        user.setUsername(u);
        user.setPasswordHash(encoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public String login(String username, String password) {
        String u = normalize(username);
        User user = findByUsername(u);
        if (user == null) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (password == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return JwtUtil.issueToken(user.getId(), user.getUsername(), jwtSecret, ttlMillis);
    }

    @Override
    public User findByUsername(String username) {
        if (username == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim();
    }
}

