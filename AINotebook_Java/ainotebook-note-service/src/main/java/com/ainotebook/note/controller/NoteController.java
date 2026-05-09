package com.ainotebook.note.controller;

import com.ainotebook.note.entity.Note;
import com.ainotebook.note.service.NoteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @GetMapping("/list")
    public List<Note> list(HttpServletRequest request) {
        Long userId = getUserId(request);
        return noteService.list(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getCreatedAt));
    }

    @PostMapping("/save")
    public boolean save(@RequestBody Note note, HttpServletRequest request) {
        Long userId = getUserId(request);
        note.setUserId(userId);
        note.setCreatedAt(LocalDateTime.now());
        return noteService.save(note);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        Note note = noteService.getById(id);
        if (note == null) throw new IllegalArgumentException("笔记不存在");
        if (!userId.equals(note.getUserId())) throw new IllegalArgumentException("无权限删除该笔记");
        return noteService.removeById(id);
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam(required = false) String apiKey, HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Note> notes = noteService.list(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getCreatedAt)
                .last("limit 5"));
        List<String> contents = notes.stream().map(Note::getContent).collect(Collectors.toList());
        return noteService.analyzeNotes(contents, apiKey);
    }

    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        if (v instanceof String) return Long.parseLong((String) v);
        if (v instanceof Long) return (Long) v;
        return Long.parseLong(String.valueOf(v));
    }
}
