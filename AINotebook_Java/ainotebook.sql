CREATE DATABASE IF NOT EXISTS ainotebook CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ainotebook;

CREATE TABLE IF NOT EXISTS notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notes_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @notes_user_id_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notes' AND COLUMN_NAME = 'user_id'
);
SET @sql_notes_user_id := IF(@notes_user_id_exists = 0, 'ALTER TABLE notes ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql_notes_user_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notes_user_id_index_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notes' AND INDEX_NAME = 'idx_notes_user_id'
);
SET @sql_notes_user_id_idx := IF(@notes_user_id_index_exists = 0, 'ALTER TABLE notes ADD INDEX idx_notes_user_id (user_id)', 'SELECT 1');
PREPARE stmt2 FROM @sql_notes_user_id_idx;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    nickname VARCHAR(64) DEFAULT '默认用户',
    avatar VARCHAR(255) DEFAULT '',
    bio VARCHAR(200) DEFAULT '这个家伙很懒，什么也没有留下',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @users_bio_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'bio'
);
SET @sql_users_bio := IF(@users_bio_exists = 0, 'ALTER TABLE users ADD COLUMN bio VARCHAR(200) DEFAULT ''这个家伙很懒，什么也没有留下''', 'SELECT 1');
PREPARE stmt3 FROM @sql_users_bio;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- 博客表
CREATE TABLE IF NOT EXISTS tb_blog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT, -- AI 自动生成的摘要
    liked INT DEFAULT 0, -- 点赞数
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 博客点赞记录（用于持久化，Redis 为主）
CREATE TABLE IF NOT EXISTS tb_blog_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blog_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blog_user (blog_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 博客评论表
CREATE TABLE IF NOT EXISTS tb_blog_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blog_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_blog_id (blog_id),
    INDEX idx_blog_comments_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @blog_comments_user_id_index_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_blog_comments' AND INDEX_NAME = 'idx_blog_comments_user_id'
);
SET @sql_blog_comments_user_id_idx := IF(@blog_comments_user_id_index_exists = 0, 'ALTER TABLE tb_blog_comments ADD INDEX idx_blog_comments_user_id (user_id)', 'SELECT 1');
PREPARE stmt4 FROM @sql_blog_comments_user_id_idx;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

-- 聊天会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(60) NOT NULL DEFAULT '新对话',
    assistant_name VARCHAR(30) NOT NULL DEFAULT '小淼',
    system_prompt VARCHAR(2000) NOT NULL DEFAULT '',
    model VARCHAR(60) NOT NULL DEFAULT '',
    base_url VARCHAR(255) NOT NULL DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_sessions_user_id (user_id),
    INDEX idx_chat_sessions_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_messages_session_id (session_id),
    INDEX idx_chat_messages_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
