package com.ainotebook.note.controller;

import com.ainotebook.note.dto.CommentItem;
import com.ainotebook.note.service.BlogCommentService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blog")
public class BlogCommentController {

    private final BlogCommentService blogCommentService;

    public BlogCommentController(BlogCommentService blogCommentService) {
        this.blogCommentService = blogCommentService;
    }

    @GetMapping("/{id}/comments")
    public List<CommentItem> list(@PathVariable("id") Long blogId,
                                  @RequestParam(defaultValue = "1") int current,
                                  @RequestParam(defaultValue = "20") int size,
                                  HttpServletRequest request) {
        Long userId = getUserId(request);
        return blogCommentService.listComments(userId, blogId, current, size);
    }

    @PostMapping("/{id}/comments")
    public Map<String, Long> add(@PathVariable("id") Long blogId,
                                 @RequestBody Map<String, String> body,
                                 HttpServletRequest request) {
        Long userId = getUserId(request);
        Long id = blogCommentService.addComment(userId, blogId, body.get("content"));
        return Map.of("id", id);
    }

    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        if (v instanceof String) return Long.parseLong((String) v);
        if (v instanceof Long) return (Long) v;
        return Long.parseLong(String.valueOf(v));
    }
}

