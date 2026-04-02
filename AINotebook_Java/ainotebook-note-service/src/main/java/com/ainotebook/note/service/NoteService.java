package com.ainotebook.note.service;

import com.ainotebook.note.entity.Note;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NoteService extends IService<Note> {
    String analyzeNotes(List<String> noteContents, String apiKey);
}
