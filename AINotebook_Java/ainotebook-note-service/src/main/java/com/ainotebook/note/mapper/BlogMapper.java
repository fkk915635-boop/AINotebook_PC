package com.ainotebook.note.mapper;

import com.ainotebook.note.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
    @Select("SELECT IFNULL(SUM(liked),0) FROM tb_blog WHERE user_id = #{userId}")
    Integer sumLikesByUserId(Long userId);
}
