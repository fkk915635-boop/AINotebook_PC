package com.ainotebook.auth.service;

import com.ainotebook.auth.entity.User;

public interface AuthService {
    void register(String username, String password);
    String login(String username, String password);
    User findByUsername(String username);
}

