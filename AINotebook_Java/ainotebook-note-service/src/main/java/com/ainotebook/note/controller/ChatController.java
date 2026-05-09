package com.ainotebook.note.controller;

import com.ainotebook.note.dto.ChatSessionItem;
import com.ainotebook.note.entity.ChatMessage;
import com.ainotebook.note.service.ChatService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public List<ChatSessionItem> sessions(HttpServletRequest request) {
        Long userId = getUserId(request);
        return chatService.listSessions(userId);
    }

    @PostMapping("/sessions")
    public Map<String, Long> createSession(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        Long id = chatService.createSession(userId, body);
        return Map.of("id", id);
    }

    @DeleteMapping("/sessions/{id}")
    public void deleteSession(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        chatService.deleteSession(userId, id);
    }

    @GetMapping("/sessions/{id}/messages")
    public List<ChatMessage> messages(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        return chatService.listMessages(userId, id);
    }

    @PostMapping("/sessions/{id}/messages")
    public Map<String, Long> append(@PathVariable("id") Long id,
                                    @RequestBody Map<String, String> body,
                                    HttpServletRequest request) {
        Long userId = getUserId(request);
        Long mid = chatService.appendMessage(userId, id, body);
        return Map.of("id", mid);
    }

    @PutMapping("/sessions/{id}")
    public void updateSession(@PathVariable("id") Long id,
                              @RequestBody Map<String, String> body,
                              HttpServletRequest request) {
        Long userId = getUserId(request);
        chatService.updateSessionConfig(userId, id, body);
    }

    @PostMapping("/sessions/{id}/clear")
    public void clearSession(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        chatService.clearSession(userId, id);
    }

    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        if (v instanceof String) return Long.parseLong((String) v);
        if (v instanceof Long) return (Long) v;
        return Long.parseLong(String.valueOf(v));
    }
}
