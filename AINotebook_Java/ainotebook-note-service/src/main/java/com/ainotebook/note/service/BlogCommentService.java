package com.ainotebook.note.service;

import com.ainotebook.note.dto.CommentItem;

import java.util.List;

public interface BlogCommentService {
    Long addComment(Long userId, Long blogId, String content);
    List<CommentItem> listComments(Long viewerId, Long blogId, int current, int size);
}

