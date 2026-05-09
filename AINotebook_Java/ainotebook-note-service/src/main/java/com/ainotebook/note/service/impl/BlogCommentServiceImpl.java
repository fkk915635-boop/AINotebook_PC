package com.ainotebook.note.service.impl;

import com.ainotebook.note.dto.CommentItem;
import com.ainotebook.note.entity.BlogComment;
import com.ainotebook.note.entity.UserProfile;
import com.ainotebook.note.mapper.BlogCommentMapper;
import com.ainotebook.note.mapper.UserProfileMapper;
import com.ainotebook.note.service.BlogCommentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlogCommentServiceImpl implements BlogCommentService {

    private final BlogCommentMapper blogCommentMapper;
    private final UserProfileMapper userProfileMapper;

    public BlogCommentServiceImpl(BlogCommentMapper blogCommentMapper, UserProfileMapper userProfileMapper) {
        this.blogCommentMapper = blogCommentMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public Long addComment(Long userId, Long blogId, String content) {
        if (userId == null) throw new IllegalArgumentException("未获取到有效的用户认证信息");
        if (blogId == null) throw new IllegalArgumentException("博客ID不能为空");
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) throw new IllegalArgumentException("评论内容不能为空");
        if (c.length() > 300) throw new IllegalArgumentException("评论内容不能超过300字");

        BlogComment bc = new BlogComment();
        bc.setBlogId(blogId);
        bc.setUserId(userId);
        bc.setContent(c);
        bc.setCreatedAt(LocalDateTime.now());
        blogCommentMapper.insert(bc);
        return bc.getId();
    }

    @Override
    public List<CommentItem> listComments(Long viewerId, Long blogId, int current, int size) {
        Page<BlogComment> page = blogCommentMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BlogComment>()
                        .eq(BlogComment::getBlogId, blogId)
                        .orderByAsc(BlogComment::getCreatedAt));
        List<BlogComment> records = page.getRecords();
        if (records == null || records.isEmpty()) return List.of();

        List<Long> userIds = records.stream().map(BlogComment::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UserProfile> userMap = userProfileMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, u -> u, (a, b) -> a));

        return records.stream().map(c -> {
            CommentItem item = new CommentItem();
            item.setId(c.getId());
            item.setBlogId(c.getBlogId());
            item.setUserId(c.getUserId());
            item.setContent(c.getContent());
            item.setCreatedAt(c.getCreatedAt());
            UserProfile u = userMap.get(c.getUserId());
            if (u != null) {
                item.setAuthorNickname(u.getNickname());
                item.setAuthorAvatar(u.getAvatar());
            }
            item.setMine(viewerId != null && viewerId.equals(c.getUserId()));
            return item;
        }).collect(Collectors.toList());
    }
}

