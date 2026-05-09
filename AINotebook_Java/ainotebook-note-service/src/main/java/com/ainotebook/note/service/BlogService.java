package com.ainotebook.note.service;

import com.ainotebook.note.entity.Blog;
import com.ainotebook.note.dto.BlogDetailItem;
import com.ainotebook.note.dto.BlogFeedItem;
import com.ainotebook.note.dto.LikeRankItem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface BlogService extends IService<Blog> {
    Long publishBlog(Long userId, String title, String content, String apiKey);
    void likeBlog(Long blogId, Long userId);
    List<Blog> queryHotBlogs(int current, int size);
    List<BlogFeedItem> queryFeed(Long viewerId, int current, int size);
    List<BlogFeedItem> queryMyBlogs(Long userId, int current, int size);
    BlogDetailItem queryDetail(Long viewerId, Long blogId);
    List<LikeRankItem> queryLikeRank(int size);
    void deleteMyBlog(Long userId, Long blogId);
    Map<String, String> optimizeContent(Long userId, String title, String content, String apiKey);
}
