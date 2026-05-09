package com.ainotebook.note.service;

import com.ainotebook.note.dto.ChatSessionItem;
import com.ainotebook.note.entity.ChatMessage;

import java.util.List;
import java.util.Map;

public interface ChatService {
    Long createSession(Long userId, Map<String, String> body);
    List<ChatSessionItem> listSessions(Long userId);
    void deleteSession(Long userId, Long sessionId);
    List<ChatMessage> listMessages(Long userId, Long sessionId);
    Long appendMessage(Long userId, Long sessionId, Map<String, String> body);
    void updateSessionConfig(Long userId, Long sessionId, Map<String, String> body);
    void clearSession(Long userId, Long sessionId);
}
