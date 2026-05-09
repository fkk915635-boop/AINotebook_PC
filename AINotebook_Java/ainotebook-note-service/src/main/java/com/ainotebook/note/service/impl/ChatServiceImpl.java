package com.ainotebook.note.service.impl;

import com.ainotebook.note.dto.ChatSessionItem;
import com.ainotebook.note.entity.ChatMessage;
import com.ainotebook.note.entity.ChatSession;
import com.ainotebook.note.mapper.ChatMessageMapper;
import com.ainotebook.note.mapper.ChatSessionMapper;
import com.ainotebook.note.service.ChatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    public ChatServiceImpl(ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public Long createSession(Long userId, Map<String, String> body) {
        if (userId == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        String title = body.getOrDefault("title", "新对话");
        String assistantName = body.getOrDefault("assistantName", "小淼");
        String systemPrompt = body.getOrDefault("systemPrompt", "");
        String model = body.getOrDefault("model", "");
        String baseUrl = body.getOrDefault("baseUrl", "");
        if (title.length() > 60) title = title.substring(0, 60);
        if (assistantName.length() > 30) assistantName = assistantName.substring(0, 30);
        if (systemPrompt.length() > 2000) systemPrompt = systemPrompt.substring(0, 2000);
        if (model.length() > 60) model = model.substring(0, 60);
        if (baseUrl.length() > 255) baseUrl = baseUrl.substring(0, 255);

        ChatSession s = new ChatSession();
        s.setUserId(userId);
        s.setTitle(title);
        s.setAssistantName(assistantName);
        s.setSystemPrompt(systemPrompt);
        s.setModel(model);
        s.setBaseUrl(baseUrl);
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(s);
        return s.getId();
    }

    @Override
    public List<ChatSessionItem> listSessions(Long userId) {
        if (userId == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        List<ChatSession> sessions = chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt)
                .orderByDesc(ChatSession::getId));
        return sessions.stream().map(s -> {
            ChatSessionItem item = new ChatSessionItem();
            item.setId(s.getId());
            item.setTitle(s.getTitle());
            item.setAssistantName(s.getAssistantName());
            item.setSystemPrompt(s.getSystemPrompt());
            item.setModel(s.getModel());
            item.setBaseUrl(s.getBaseUrl());
            item.setCreatedAt(s.getCreatedAt());
            item.setUpdatedAt(s.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession s = mustOwnSession(userId, sessionId);
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, s.getId()));
        chatSessionMapper.deleteById(s.getId());
    }

    @Override
    public List<ChatMessage> listMessages(Long userId, Long sessionId) {
        mustOwnSession(userId, sessionId);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt)
                .orderByAsc(ChatMessage::getId));
    }

    @Override
    public Long appendMessage(Long userId, Long sessionId, Map<String, String> body) {
        ChatSession s = mustOwnSession(userId, sessionId);
        String role = body.getOrDefault("role", "").trim();
        String content = body.getOrDefault("content", "").trim();
        if (!role.equals("user") && !role.equals("assistant") && !role.equals("system")) {
            throw new IllegalArgumentException("非法角色");
        }
        if (content.isEmpty()) throw new IllegalArgumentException("消息不能为空");
        if (content.length() > 8000) content = content.substring(0, 8000);

        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(m);

        if (s.getTitle() == null || s.getTitle().equals("新对话")) {
            if (role.equals("user")) {
                String t = content.replaceAll("\\s+", " ");
                if (t.length() > 18) t = t.substring(0, 18) + "…";
                ChatSession upd = new ChatSession();
                upd.setId(sessionId);
                upd.setTitle(t);
                upd.setUpdatedAt(LocalDateTime.now());
                chatSessionMapper.updateById(upd);
            } else {
                ChatSession upd = new ChatSession();
                upd.setId(sessionId);
                upd.setUpdatedAt(LocalDateTime.now());
                chatSessionMapper.updateById(upd);
            }
        } else {
            ChatSession upd = new ChatSession();
            upd.setId(sessionId);
            upd.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(upd);
        }

        return m.getId();
    }

    @Override
    public void updateSessionConfig(Long userId, Long sessionId, Map<String, String> body) {
        mustOwnSession(userId, sessionId);
        ChatSession upd = new ChatSession();
        upd.setId(sessionId);
        String title = body.get("title");
        String assistantName = body.get("assistantName");
        String systemPrompt = body.get("systemPrompt");
        String model = body.get("model");
        String baseUrl = body.get("baseUrl");
        if (title != null) {
            String t = title.trim();
            if (t.length() > 60) t = t.substring(0, 60);
            upd.setTitle(t.isEmpty() ? "新对话" : t);
        }
        if (assistantName != null) {
            String n = assistantName.trim();
            if (n.length() > 30) n = n.substring(0, 30);
            upd.setAssistantName(n.isEmpty() ? "小淼" : n);
        }
        if (systemPrompt != null) {
            String p = systemPrompt.trim();
            if (p.length() > 2000) p = p.substring(0, 2000);
            upd.setSystemPrompt(p);
        }
        if (model != null) {
            String m = model.trim();
            if (m.length() > 60) m = m.substring(0, 60);
            upd.setModel(m);
        }
        if (baseUrl != null) {
            String b = baseUrl.trim();
            if (b.length() > 255) b = b.substring(0, 255);
            upd.setBaseUrl(b);
        }
        upd.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(upd);
    }

    @Override
    public void clearSession(Long userId, Long sessionId) {
        mustOwnSession(userId, sessionId);
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId));
        ChatSession upd = new ChatSession();
        upd.setId(sessionId);
        upd.setTitle("新对话");
        upd.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(upd);
    }

    private ChatSession mustOwnSession(Long userId, Long sessionId) {
        if (userId == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        if (sessionId == null) throw new IllegalArgumentException("会话ID不能为空");
        ChatSession s = chatSessionMapper.selectById(sessionId);
        if (s == null) throw new IllegalArgumentException("会话不存在");
        if (!Objects.equals(s.getUserId(), userId)) throw new IllegalArgumentException("无权限访问该会话");
        return s;
    }
}
