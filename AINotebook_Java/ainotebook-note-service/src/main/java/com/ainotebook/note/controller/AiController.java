package com.ainotebook.note.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Value("${ainotebook.ollama.baseUrl:http://localhost:11434}")
    private String baseUrl;

    @Value("${ainotebook.ollama.model:deepseek}")
    private String defaultModel;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest req) {
        String model = (req.model == null || req.model.trim().isEmpty()) ? defaultModel : req.model.trim();
        String runtimeBaseUrl = normalizeBaseUrl(req.baseUrl);
        if (runtimeBaseUrl == null) runtimeBaseUrl = baseUrl;
        String userName = req.userName == null ? "" : req.userName.trim();
        String assistantName = req.assistantName == null ? "" : req.assistantName.trim();
        String systemPrompt = req.systemPrompt == null ? "" : req.systemPrompt.trim();
        List<Message> in = req.messages == null ? List.of() : req.messages;

        List<Map<String, String>> messages = new ArrayList<>();
        String system = buildSystem(systemPrompt, assistantName, userName);
        messages.add(Map.of("role", "system", "content", system));
        for (Message m : in) {
            if (m == null) continue;
            String role = m.role == null ? "" : m.role.trim();
            String content = m.content == null ? "" : m.content;
            if (!role.equals("user") && !role.equals("assistant") && !role.equals("system")) continue;
            if (content.trim().isEmpty()) continue;
            messages.add(Map.of("role", role, "content", content));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "stream", false
        );

        try {
            String response = HttpRequest.post(runtimeBaseUrl + "/api/chat")
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(120000)
                    .execute()
                    .body();

            JSONObject obj = JSONUtil.parseObj(response);
            String content = obj.getByPath("message.content", String.class);
            if (content == null) content = "";
            return Map.of("content", content, "model", model);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            throw new IllegalArgumentException("无法连接到本地 Ollama，请确认 Ollama 正在运行且模型可用。" + (msg.isEmpty() ? "" : (" 详情：" + msg)));
        }
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest req) {
        String model = (req.model == null || req.model.trim().isEmpty()) ? defaultModel : req.model.trim();
        String runtimeBaseUrl = normalizeBaseUrl(req.baseUrl);
        if (runtimeBaseUrl == null) runtimeBaseUrl = baseUrl;
        String userName = req.userName == null ? "" : req.userName.trim();
        String assistantName = req.assistantName == null ? "" : req.assistantName.trim();
        String systemPrompt = req.systemPrompt == null ? "" : req.systemPrompt.trim();
        List<Message> in = req.messages == null ? List.of() : req.messages;

        List<Map<String, String>> messages = new ArrayList<>();
        String system = buildSystem(systemPrompt, assistantName, userName);
        messages.add(Map.of("role", "system", "content", system));
        for (Message m : in) {
            if (m == null) continue;
            String role = m.role == null ? "" : m.role.trim();
            String content = m.content == null ? "" : m.content;
            if (!role.equals("user") && !role.equals("assistant") && !role.equals("system")) continue;
            if (content.trim().isEmpty()) continue;
            messages.add(Map.of("role", role, "content", content));
        }

        SseEmitter emitter = new SseEmitter(180000L);
        String finalRuntimeBaseUrl = runtimeBaseUrl;
        new Thread(() -> {
            try {
                Map<String, Object> body = Map.of(
                        "model", model,
                        "messages", messages,
                        "stream", true
                );

                cn.hutool.http.HttpResponse resp = HttpRequest.post(finalRuntimeBaseUrl + "/api/chat")
                        .header("Content-Type", "application/json")
                        .body(JSONUtil.toJsonStr(body))
                        .timeout(0)
                        .execute(true);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(resp.bodyStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String s = line.trim();
                        if (s.isEmpty()) continue;
                        try {
                            JSONObject obj = JSONUtil.parseObj(s);
                            String delta = obj.getByPath("message.content", String.class);
                            Boolean done = obj.getBool("done");
                            if (delta != null && !delta.isEmpty()) {
                                emitter.send(SseEmitter.event().name("delta").data(delta));
                            }
                            if (Boolean.TRUE.equals(done)) break;
                        } catch (Exception ignored) {
                        }
                    }
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    String msg = e.getMessage() == null ? "" : e.getMessage();
                    emitter.send(SseEmitter.event().name("error").data("无法连接到本地 Ollama。" + (msg.isEmpty() ? "" : (" 详情：" + msg))));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        }, "ollama-chat-stream").start();

        return emitter;
    }

    private String buildSystem(String systemPrompt, String assistantName, String userName) {
        String s = systemPrompt == null ? "" : systemPrompt.trim();
        if (s.length() > 2000) {
            s = s.substring(0, 2000);
        }
        if (s.isEmpty()) {
            s = "你是 AINotebook 的本地 AI 助手，回答使用中文，语气专业、简洁、可执行。";
        }
        if (assistantName != null && !assistantName.isEmpty()) {
            String name = assistantName.length() > 30 ? assistantName.substring(0, 30) : assistantName;
            s = "你的名字是「" + name + "」。" + s;
        }
        if (userName != null && !userName.isEmpty()) {
            String u = userName.length() > 30 ? userName.substring(0, 30) : userName;
            s = s + " 你称呼用户为「" + u + "」。";
        }
        return s;
    }

    private String normalizeBaseUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        String lower = s.toLowerCase();
        if (!(lower.startsWith("http://localhost") || lower.startsWith("http://127.0.0.1"))) {
            throw new IllegalArgumentException("仅允许配置本机 Ollama 地址（localhost / 127.0.0.1）");
        }
        return s;
    }

    public static class ChatRequest {
        public String model;
        public String baseUrl;
        public String userName;
        public String assistantName;
        public String systemPrompt;
        public List<Message> messages;
    }

    public static class Message {
        public String role;
        public String content;
    }
}
