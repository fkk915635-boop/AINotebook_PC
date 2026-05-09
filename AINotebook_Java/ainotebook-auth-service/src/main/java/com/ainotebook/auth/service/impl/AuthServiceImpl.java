package com.ainotebook.auth.service.impl;

import com.ainotebook.auth.entity.User;
import com.ainotebook.auth.mapper.UserMapper;
import com.ainotebook.auth.service.AuthService;
import com.ainotebook.common.security.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    
    // 模拟 Redis 缓存验证码
    private final Map<String, String> codeCache = new ConcurrentHashMap<>();

    @Value("${ainotebook.jwt.secret:ainotebook-dev-secret}")
    private String jwtSecret;

    @Value("${ainotebook.jwt.ttlMillis:604800000}")
    private long ttlMillis;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    public AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void sendCode(String phone) {
        String p = normalize(phone);
        if (!PHONE_PATTERN.matcher(p).matches()) {
            throw new IllegalArgumentException("手机号码格式不正确");
        }
        // 模拟生成6位验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        codeCache.put(p, code);
        // 这里只是为了开发方便打印在控制台
        System.out.println("【短信服务】向手机号 " + p + " 发送了验证码: " + code);
    }

    @Override
    public String loginOrRegister(String phone, String code) {
        String p = normalize(phone);
        String c = normalize(code);
        
        if (!PHONE_PATTERN.matcher(p).matches()) {
            throw new IllegalArgumentException("手机号码格式不正确");
        }
        if (c.isEmpty() || !c.equals(codeCache.get(p))) {
            // 测试环境：123456 作为万能验证码
            if (!"123456".equals(c)) {
                throw new IllegalArgumentException("验证码错误或已过期");
            }
        }
        
        // 验证成功后移除验证码
        codeCache.remove(p);

        User user = findByPhone(p);
        if (user == null) {
            // 自动注册
            user = new User();
            user.setPhone(p);
            user.setNickname("用户_" + p.substring(7));
            user.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=" + p);
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }
        
        return JwtUtil.issueToken(user.getId(), user.getPhone(), jwtSecret, ttlMillis);
    }

    @Override
    public User findByPhone(String phone) {
        if (phone == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    @Override
    public User findById(Long id) {
        if (id == null) return null;
        return userMapper.selectById(id);
    }

    @Override
    public void updateProfile(Long id, String nickname, String avatar, String bio) {
        if (id == null) throw new IllegalArgumentException("用户ID不能为空");
        User user = new User();
        user.setId(id);
        if (nickname != null) {
            String n = nickname.trim();
            if (n.length() < 2 || n.length() > 10) {
                throw new IllegalArgumentException("昵称长度必须在2-10个字符之间");
            }
            user.setNickname(n);
        }
        if (avatar != null) {
            user.setAvatar(avatar.trim());
        }
        if (bio != null) {
            String b = bio.trim();
            if (b.length() > 200) {
                throw new IllegalArgumentException("个人简介不能超过200字");
            }
            user.setBio(b.isEmpty() ? "这个家伙很懒，什么也没有留下" : b);
        }
        userMapper.updateById(user);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim();
    }
}
