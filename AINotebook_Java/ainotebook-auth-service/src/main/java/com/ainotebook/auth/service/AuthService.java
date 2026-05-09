package com.ainotebook.auth.service;

import com.ainotebook.auth.entity.User;

public interface AuthService {
    String loginOrRegister(String phone, String code);
    void sendCode(String phone);
    User findByPhone(String phone);
    User findById(Long id);
    void updateProfile(Long id, String nickname, String avatar, String bio);
}
