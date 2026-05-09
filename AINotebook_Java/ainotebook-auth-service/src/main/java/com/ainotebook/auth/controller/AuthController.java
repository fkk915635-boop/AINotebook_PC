package com.ainotebook.auth.controller;

import com.ainotebook.auth.dto.LoginRequest;
import com.ainotebook.auth.dto.TokenResponse;
import com.ainotebook.auth.service.AuthService;
import com.ainotebook.auth.entity.User;
import com.ainotebook.common.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final String jwtSecret;

    public AuthController(AuthService authService, @org.springframework.beans.factory.annotation.Value("${ainotebook.jwt.secret:ainotebook-dev-secret}") String jwtSecret) {
        this.authService = authService;
        this.jwtSecret = jwtSecret;
    }

    @PostMapping("/send-code")
    public void sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        authService.sendCode(phone);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest req) {
        String token = authService.loginOrRegister(req.getPhone(), req.getCode());
        return new TokenResponse(token);
    }

    @GetMapping("/me")
    public User getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        return authService.findById(userId);
    }

    @PutMapping("/profile")
    public void updateProfile(@RequestHeader(value = "Authorization", required = false) String token, @RequestBody Map<String, String> body) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        String nickname = body.get("nickname");
        String avatar = body.get("avatar");
        String bio = body.get("bio");
        authService.updateProfile(userId, nickname, avatar, bio);
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                io.jsonwebtoken.Claims claims = JwtUtil.parseClaims(token, jwtSecret);
                return Long.parseLong(claims.getSubject());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadAvatar(@RequestHeader(value = "Authorization", required = false) String token,
                                            @RequestPart("file") MultipartFile file) throws Exception {
        Long userId = getUserIdFromToken(token);
        if (userId == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择头像文件");
        if (file.getSize() > 2L * 1024 * 1024) throw new IllegalArgumentException("头像文件不能超过 2MB");

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("仅支持图片格式头像");
        }
        if (!contentType.equalsIgnoreCase("image/png")
                && !contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/jpg")
                && !contentType.equalsIgnoreCase("image/webp")) {
            throw new IllegalArgumentException("仅支持 png/jpg/webp 格式头像");
        }

        String ext = ".png";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String e = original.substring(original.lastIndexOf('.')).toLowerCase();
            if (e.length() <= 8) ext = e;
        }

        Path dir = Path.of(System.getProperty("user.dir"), "uploads", "avatars");
        Files.createDirectories(dir);
        String filename = "u" + userId + "_" + System.currentTimeMillis() + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());

        String avatarUrl = "/api/auth/uploads/avatars/" + filename;
        authService.updateProfile(userId, null, avatarUrl, null);

        return Map.of("avatar", avatarUrl);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException e) {
        return new ErrorResponse(e.getMessage());
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse() {
        }

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
