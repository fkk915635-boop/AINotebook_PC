package com.ainotebook.note.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ainotebook.note.entity.Note;
import com.ainotebook.note.mapper.NoteMapper;
import com.ainotebook.note.service.NoteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Override
    public String analyzeNotes(List<String> noteContents, String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return simulateAiAnalysis();
        }

        String context = String.join("\n", noteContents);
        String prompt = "你是一位专业的思维分析师，请基于用户最近的思考记录，生成一份深度洞察报告。\n\n" +
                "要求：\n1. 用中文输出，语气温暖、有启发性\n2. 分析维度：\n" +
                "   - 核心主题（1-2 个高频关键词）\n" +
                "   - 情绪倾向（积极/平静/焦虑/困惑等）\n" +
                "   - 思维模式（如：反思型、规划型、创意型）\n" +
                "   - 1 个启发性问题（引导用户深度思考）\n" +
                "3. 格式简洁，避免冗长\n\n用户最近的思考记录：\n" + context;

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", JSONUtil.createArray().set(JSONUtil.createObj().set("role", "user").set("content", prompt)));
        body.put("temperature", 0.7);
        body.put("max_tokens", 500);

        try {
            String response = HttpRequest.post("https://api.deepseek.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(15000)
                    .execute().body();

            JSONObject jsonResponse = JSONUtil.parseObj(response);
            return jsonResponse.getByPath("choices[0].message.content", String.class);
        } catch (Exception e) {
            return "AI 分析失败: " + e.getMessage();
        }
    }

    private String simulateAiAnalysis() {
        return "🧠 AI 洞察报告（模拟模式）\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "核心主题：自我成长\n" +
                "情绪画像：😌 内心平和，理性思考\n" +
                "深度洞察：你最近的思考更偏向长期规划\n\n" +
                "💡 行动建议：\n" +
                "• 每周回顾一次笔记，发现思维模式变化\n" +
                "• 对高频主题做专项记录（如「情绪日记」）\n" +
                "• 尝试用语音输入提升记录效率\n\n" +
                "✨ 提示：配置 DeepSeek API Key 可获得真实 AI 分析";
    }
}
