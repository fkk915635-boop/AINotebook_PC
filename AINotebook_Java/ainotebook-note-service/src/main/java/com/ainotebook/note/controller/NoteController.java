package com.ainotebook.note.controller;

import com.ainotebook.note.entity.Note;
import com.ainotebook.note.service.NoteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @GetMapping("/list")
    public List<Note> list() {
        return noteService.list(new LambdaQueryWrapper<Note>().orderByDesc(Note::getCreatedAt));
    }

    @PostMapping("/save")
    public boolean save(@RequestBody Note note) {
        note.setCreatedAt(LocalDateTime.now());
        return noteService.save(note);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return noteService.removeById(id);
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam(required = false) String apiKey) {
        List<Note> notes = noteService.list(new LambdaQueryWrapper<Note>()
                .orderByDesc(Note::getCreatedAt)
                .last("limit 5"));
        List<String> contents = notes.stream().map(Note::getContent).collect(Collectors.toList());
        return noteService.analyzeNotes(contents, apiKey);
    }
}
