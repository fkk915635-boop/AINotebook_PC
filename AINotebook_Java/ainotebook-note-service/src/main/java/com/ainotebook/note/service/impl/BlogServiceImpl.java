package com.ainotebook.note.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ainotebook.note.dto.BlogDetailItem;
import com.ainotebook.note.dto.BlogFeedItem;
import com.ainotebook.note.dto.LikeRankItem;
import com.ainotebook.note.entity.Blog;
import com.ainotebook.note.entity.UserProfile;
import com.ainotebook.note.mapper.BlogMapper;
import com.ainotebook.note.service.BlogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private com.ainotebook.note.mapper.UserProfileMapper userProfileMapper;

    @Autowired
    private com.ainotebook.note.service.FollowService followService;

    @Override
    public Long publishBlog(Long userId, String title, String content, String apiKey) {
        Blog blog = new Blog();
        blog.setUserId(userId);
        blog.setTitle(title);
        blog.setContent(content);
        
        // 调用 AI 生成摘要
        String summary = generateAiSummary(content, apiKey);
        blog.setSummary(summary);
        
        save(blog);
        return blog.getId();
    }

    @Override
    public void likeBlog(Long blogId, Long userId) {
        // 1. 判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blogId;
        Boolean isMember;
        try {
            isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        } catch (Exception e) {
            // Redis 异常时降级为“仅数据库点赞+1”
            update().setSql("liked = liked + 1").eq("id", blogId).update();
            return;
        }
        
        if (Boolean.FALSE.equals(isMember)) {
            // 2. 如果未点赞，可以点赞
            // 2.1 数据库点赞数 +1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", blogId).update();
            // 2.2 保存用户到 Redis 的 set 集合
            if (isSuccess) {
                try {
                    stringRedisTemplate.opsForSet().add(key, userId.toString());
                } catch (Exception ignored) {
                }
            }
        } else {
            // 3. 如果已点赞，取消点赞
            // 3.1 数据库点赞数 -1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", blogId).update();
            // 3.2 把用户从 Redis 的 set 集合移除
            if (isSuccess) {
                try {
                    stringRedisTemplate.opsForSet().remove(key, userId.toString());
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public List<Blog> queryHotBlogs(int current, int size) {
        // 根据点赞数排序分页查询
        Page<Blog> page = query().orderByDesc("liked").page(new Page<>(current, size));
        return page.getRecords();
    }

    @Override
    public List<BlogFeedItem> queryFeed(Long viewerId, int current, int size) {
        Page<Blog> page = query().orderByDesc("created_at").page(new Page<>(current, size));
        List<Blog> records = page.getRecords();
        if (records == null || records.isEmpty()) return List.of();

        List<Long> userIds = records.stream().map(Blog::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UserProfile> userMap = userProfileMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, u -> u, (a, b) -> a));

        return records.stream().map(b -> {
            BlogFeedItem item = new BlogFeedItem();
            item.setId(b.getId());
            item.setUserId(b.getUserId());
            item.setTitle(b.getTitle());
            item.setContent(b.getContent());
            item.setSummary(b.getSummary());
            item.setLiked(b.getLiked());
            item.setCreatedAt(b.getCreatedAt());

            UserProfile u = userMap.get(b.getUserId());
            if (u != null) {
                item.setAuthorNickname(u.getNickname());
                item.setAuthorAvatar(u.getAvatar());
            }
            item.setMine(viewerId != null && viewerId.equals(b.getUserId()));
            item.setFollowed(safeIsFollowed(viewerId, b.getUserId()));
            item.setLikedByMe(safeLikedByMe(viewerId, b.getId()));
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BlogFeedItem> queryMyBlogs(Long userId, int current, int size) {
        Page<Blog> page = query().eq("user_id", userId).orderByDesc("created_at").page(new Page<>(current, size));
        List<Blog> records = page.getRecords();
        if (records == null || records.isEmpty()) return List.of();
        UserProfile me = userProfileMapper.selectById(userId);
        return records.stream().map(b -> {
            BlogFeedItem item = new BlogFeedItem();
            item.setId(b.getId());
            item.setUserId(b.getUserId());
            item.setTitle(b.getTitle());
            item.setContent(b.getContent());
            item.setSummary(b.getSummary());
            item.setLiked(b.getLiked());
            item.setCreatedAt(b.getCreatedAt());
            item.setMine(true);
            item.setFollowed(false);
            item.setLikedByMe(safeLikedByMe(userId, b.getId()));
            if (me != null) {
                item.setAuthorNickname(me.getNickname());
                item.setAuthorAvatar(me.getAvatar());
            }
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public BlogDetailItem queryDetail(Long viewerId, Long blogId) {
        Blog b = getById(blogId);
        if (b == null) throw new IllegalArgumentException("博客不存在");
        UserProfile u = userProfileMapper.selectById(b.getUserId());

        BlogDetailItem item = new BlogDetailItem();
        item.setId(b.getId());
        item.setUserId(b.getUserId());
        item.setTitle(b.getTitle());
        item.setContent(b.getContent());
        item.setSummary(b.getSummary());
        item.setLiked(b.getLiked());
        item.setCreatedAt(b.getCreatedAt());
        if (u != null) {
            item.setAuthorNickname(u.getNickname());
            item.setAuthorAvatar(u.getAvatar());
        }
        item.setMine(viewerId != null && viewerId.equals(b.getUserId()));
        item.setFollowed(safeIsFollowed(viewerId, b.getUserId()));
        item.setLikedByMe(safeLikedByMe(viewerId, b.getId()));
        return item;
    }

    @Override
    public List<LikeRankItem> queryLikeRank(int size) {
        Page<Blog> page = query().orderByDesc("liked").page(new Page<>(1, size));
        List<Blog> records = page.getRecords();
        if (records == null || records.isEmpty()) return List.of();

        List<Long> userIds = records.stream().map(Blog::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UserProfile> userMap = userProfileMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, u -> u, (a, b) -> a));

        return records.stream().map(b -> {
            LikeRankItem item = new LikeRankItem();
            item.setId(b.getId());
            item.setTitle(b.getTitle());
            item.setLiked(b.getLiked());
            item.setUserId(b.getUserId());
            UserProfile u = userMap.get(b.getUserId());
            if (u != null) {
                item.setAuthorNickname(u.getNickname());
                item.setAuthorAvatar(u.getAvatar());
            }
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteMyBlog(Long userId, Long blogId) {
        Blog blog = getById(blogId);
        if (blog == null) throw new IllegalArgumentException("博客不存在");
        if (userId == null || !userId.equals(blog.getUserId())) throw new IllegalArgumentException("无权限删除该博客");
        removeById(blogId);
        try {
            stringRedisTemplate.delete("blog:liked:" + blogId);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Map<String, String> optimizeContent(Long userId, String title, String content, String apiKey) {
        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("标题不能为空");
        if (c.isEmpty()) throw new IllegalArgumentException("内容不能为空");

        if (apiKey == null || apiKey.isEmpty()) {
            return Map.of(
                    "title", t.replaceAll("\\s+", " "),
                    "content", c.replaceAll("\\s+\\n", "\n").replaceAll("\\n\\s+", "\n").replaceAll("[ \\t]{2,}", " ")
            );
        }

        String prompt = "请对以下博客标题和正文进行“内容优化”，要求：\n" +
                "1) 保持原意，不要添加不存在的事实\n" +
                "2) 纠正错别字、语法、标点\n" +
                "3) 优化语义连贯性和表达\n" +
                "4) 输出 JSON：{\"title\":\"...\",\"content\":\"...\"}\n\n" +
                "标题：\n" + t + "\n\n正文：\n" + c;

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", JSONUtil.createArray().set(JSONUtil.createObj().set("role", "user").set("content", prompt)));
        body.put("temperature", 0.3);
        body.put("max_tokens", 800);

        try {
            String response = HttpRequest.post("https://api.deepseek.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(15000)
                    .execute().body();

            JSONObject jsonResponse = JSONUtil.parseObj(response);
            String text = jsonResponse.getByPath("choices[0].message.content", String.class);
            if (text == null || text.isEmpty()) return Map.of("title", t, "content", c);
            String json = text.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
            JSONObject obj = JSONUtil.parseObj(json);
            String outTitle = obj.getStr("title", t);
            String outContent = obj.getStr("content", c);
            return Map.of("title", outTitle, "content", outContent);
        } catch (Exception e) {
            return Map.of("title", t, "content", c);
        }
    }

    private String generateAiSummary(String content, String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }

        String prompt = "请为以下这篇思维笔记生成一段简短、吸引人的摘要（50字以内），用于社区展示：\n\n" + content;

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", JSONUtil.createArray().set(JSONUtil.createObj().set("role", "user").set("content", prompt)));
        body.put("temperature", 0.7);
        body.put("max_tokens", 100);

        try {
            String response = HttpRequest.post("https://api.deepseek.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(10000)
                    .execute().body();

            JSONObject jsonResponse = JSONUtil.parseObj(response);
            return jsonResponse.getByPath("choices[0].message.content", String.class);
        } catch (Exception e) {
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }
    }

    private boolean safeIsFollowed(Long viewerId, Long targetUserId) {
        try {
            return followService.isFollowed(viewerId, targetUserId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean safeLikedByMe(Long viewerId, Long blogId) {
        try {
            Boolean likedByMe = stringRedisTemplate.opsForSet().isMember("blog:liked:" + blogId, String.valueOf(viewerId));
            return Boolean.TRUE.equals(likedByMe);
        } catch (Exception e) {
            return false;
        }
    }
}
