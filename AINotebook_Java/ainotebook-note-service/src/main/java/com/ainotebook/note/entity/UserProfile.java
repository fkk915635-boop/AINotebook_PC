package com.ainotebook.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class UserProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private LocalDateTime createdAt;
}

