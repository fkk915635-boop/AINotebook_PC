package com.ainotebook.note.controller;

import com.ainotebook.note.entity.Blog;
import com.ainotebook.note.service.BlogService;
import com.ainotebook.common.security.JwtUtil;
import com.ainotebook.note.dto.BlogDetailItem;
import com.ainotebook.note.dto.BlogFeedItem;
import com.ainotebook.note.dto.LikeRankItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private JwtUtil jwtUtil;

    @org.springframework.beans.factory.annotation.Value("${ainotebook.jwt.secret}")
    private String secret;

    @PostMapping("/publish")
    public Long publish(@RequestBody Blog blog, @RequestParam(required = false) String apiKey, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return blogService.publishBlog(userId, blog.getTitle(), blog.getContent(), apiKey);
    }

    @PostMapping("/like/{id}")
    public void like(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        blogService.likeBlog(id, userId);
    }

    @GetMapping("/hot")
    public List<Blog> hot(@RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "10") int size) {
        return blogService.queryHotBlogs(current, size);
    }

    @GetMapping("/feed")
    public List<BlogFeedItem> feed(@RequestParam(defaultValue = "1") int current,
                                   @RequestParam(defaultValue = "10") int size,
                                   HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return blogService.queryFeed(userId, current, size);
    }

    @GetMapping("/my")
    public List<BlogFeedItem> my(@RequestParam(defaultValue = "1") int current,
                                 @RequestParam(defaultValue = "20") int size,
                                 HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return blogService.queryMyBlogs(userId, current, size);
    }

    @GetMapping("/{id}")
    public BlogDetailItem detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return blogService.queryDetail(userId, id);
    }

    @GetMapping("/like-rank")
    public List<LikeRankItem> likeRank(@RequestParam(defaultValue = "10") int size) {
        return blogService.queryLikeRank(size);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        blogService.deleteMyBlog(userId, id);
    }

    @PostMapping("/optimize")
    public Map<String, String> optimize(@RequestBody Map<String, String> body,
                                        @RequestParam(required = false) String apiKey,
                                        HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        String title = body.get("title");
        String content = body.get("content");
        return blogService.optimizeContent(userId, title, content, apiKey);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserId(token, secret);
    }
}
