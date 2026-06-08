CREATE DATABASE IF NOT EXISTS shortvideo;

USE shortvideo;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    avatar VARCHAR(500),
    bio VARCHAR(500),
    followers INT DEFAULT 0,
    following INT DEFAULT 0,
    max_video_count INT DEFAULT 50,
    daily_upload_limit INT DEFAULT 5,
    max_storage_bytes BIGINT DEFAULT 5368709120,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    cover_url VARCHAR(500),
    video_url VARCHAR(500) NOT NULL,
    duration INT DEFAULT 0,
    file_size BIGINT DEFAULT 0,
    like_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    heat_score DOUBLE DEFAULT 0,
    last_heat_update DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_heat_score (heat_score),
    INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    is_canonical BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tag_synonyms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    canonical_tag_id BIGINT NOT NULL,
    synonym_tag_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_synonym_tag (synonym_tag_id),
    FOREIGN KEY (canonical_tag_id) REFERENCES tags(id),
    FOREIGN KEY (synonym_tag_id) REFERENCES tags(id),
    INDEX idx_canonical_tag (canonical_tag_id)
);

CREATE TABLE IF NOT EXISTS video_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_video_tag (video_id, tag_id),
    FOREIGN KEY (video_id) REFERENCES videos(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);

CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_video (user_id, video_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (video_id) REFERENCES videos(id)
);

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (video_id) REFERENCES videos(id)
);

CREATE TABLE IF NOT EXISTS watch_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    progress_seconds INT DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_video (user_id, video_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (video_id) REFERENCES videos(id)
);

CREATE TABLE IF NOT EXISTS video_milestones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    timestamp_seconds INT NOT NULL,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES videos(id),
    INDEX idx_video_id (video_id)
);

CREATE TABLE IF NOT EXISTS video_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    description TEXT,
    cover_url VARCHAR(500),
    video_url VARCHAR(500),
    video_file_name VARCHAR(200),
    duration INT DEFAULT 0,
    file_size BIGINT DEFAULT 0,
    tags_text TEXT,
    file_status VARCHAR(20) DEFAULT 'not_uploaded',
    status VARCHAR(20) DEFAULT 'draft',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id_status (user_id, status)
);

INSERT INTO users (username, email, password, bio, followers, following, max_video_count, daily_upload_limit, max_storage_bytes) VALUES 
('testuser', 'test@example.com', 'password', '这是一个测试用户', 100, 50, 50, 5, 5368709120);

INSERT INTO videos (user_id, title, description, cover_url, video_url, duration, file_size, status) VALUES 
(1, '美食打卡：红烧肉制作', '今天做了一道美味的红烧肉，分享给大家！', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=delicious%20braised%20pork%20food%20photography&image_size=square', 'https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4', 126, 15728640, 'approved'),
(1, '旅行日记：云南大理', '美丽的大理风光，让人沉醉', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=beautiful%20dali%20yunnan%20landscape%20travel&image_size=square', 'https://www.w3schools.com/html/movie.mp4', 13, 2097152, 'approved'),
(1, '健身打卡：每日一练', '坚持健身第30天，加油！', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=fitness%20workout%20gym%20exercise&image_size=square', 'https://www.w3schools.com/html/mov_bbb.mp4', 10, 1048576, 'approved'),
(1, '学习分享：编程入门', '从零开始学编程', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=programming%20coding%20computer%20science&image_size=square', 'https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4', 126, 15728640, 'approved'),
(1, '音乐翻唱：夜曲', '翻唱周杰伦的经典歌曲', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=music%20singing%20microphone%20performance&image_size=square', 'https://www.w3schools.com/html/mov_bbb.mp4', 10, 1048576, 'approved');

INSERT INTO tags (name, is_canonical) VALUES 
('美食', TRUE), ('旅行', TRUE), ('健身', TRUE), ('学习', TRUE), ('音乐', TRUE),
('美食打卡', FALSE), ('健身打卡', FALSE), ('跑步', FALSE), ('夜跑', FALSE),
('减脂餐', FALSE), ('健身餐', FALSE), ('瑜伽', FALSE), ('晨跑', FALSE),
('读书', FALSE), ('编程', FALSE), ('知识分享', FALSE), ('学习打卡', FALSE),
('翻唱', FALSE), ('原创音乐', FALSE), ('唱歌', FALSE),
('旅行日记', FALSE), ('旅游', FALSE), ('风景', FALSE);

INSERT INTO tag_synonyms (canonical_tag_id, synonym_tag_id) VALUES 
(1, 6),
(3, 7), (3, 8), (3, 9), (3, 10), (3, 11), (3, 12), (3, 13),
(4, 14), (4, 15), (4, 16), (4, 17),
(5, 18), (5, 19), (5, 20),
(2, 21), (2, 22), (2, 23);

INSERT INTO video_tags (video_id, tag_id) VALUES 
(1, 1), (2, 2), (3, 3), (4, 4), (5, 5);

INSERT INTO comments (user_id, video_id, content) VALUES 
(1, 1, '看起来好好吃！'),
(1, 1, '求教程！'),
(1, 2, '好美的风景'),
(1, 3, '加油！坚持就是胜利');

INSERT INTO watch_progress (user_id, video_id, progress_seconds, is_completed) VALUES 
(1, 1, 45, FALSE),
(1, 2, 7, FALSE),
(1, 3, 10, TRUE),
(1, 5, 3, FALSE);

INSERT INTO video_milestones (video_id, title, description, timestamp_seconds, sort_order) VALUES 
(1, '准备食材', '准备五花肉、调料等食材', 10, 1),
(1, '焯水去腥', '将五花肉焯水去除血沫', 30, 2),
(1, '炒糖色', '冰糖炒制焦糖色', 50, 3),
(1, '炖煮入味', '小火慢炖让肉入味', 80, 4),
(1, '收汁出锅', '大火收汁即可出锅', 110, 5),
(3, '热身运动', '开始前的热身准备', 2, 1),
(3, '核心训练', '腹部核心肌群训练', 5, 2),
(3, '拉伸放松', '训练后的拉伸放松', 8, 3),
(4, '环境搭建', '开发环境的安装与配置', 15, 1),
(4, 'Hello World', '第一个程序编写', 40, 2),
(4, '变量与类型', '讲解变量和数据类型', 70, 3),
(4, '条件语句', 'if-else 语句详解', 95, 4),
(4, '循环结构', 'for 和 while 循环', 115, 5);

CREATE TABLE IF NOT EXISTS tag_heat_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_time DATETIME NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_name VARCHAR(50) NOT NULL,
    heat_score DOUBLE DEFAULT 0,
    video_count INT DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    rank_order INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_snapshot_time (snapshot_time),
    INDEX idx_snapshot_rank (snapshot_time, rank_order),
    INDEX idx_tag_id (tag_id)
);

CREATE TABLE IF NOT EXISTS video_heat_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_time DATETIME NOT NULL,
    video_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    cover_url VARCHAR(500),
    author_id BIGINT NOT NULL,
    author_name VARCHAR(50),
    heat_score DOUBLE DEFAULT 0,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    rank_order INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_snapshot_time (snapshot_time),
    INDEX idx_snapshot_rank (snapshot_time, rank_order),
    INDEX idx_video_id (video_id)
);

CREATE TABLE IF NOT EXISTS author_heat_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_time DATETIME NOT NULL,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(50) NOT NULL,
    avatar VARCHAR(500),
    bio VARCHAR(500),
    heat_score DOUBLE DEFAULT 0,
    video_count INT DEFAULT 0,
    total_view_count BIGINT DEFAULT 0,
    total_like_count BIGINT DEFAULT 0,
    total_favorite_count BIGINT DEFAULT 0,
    followers INT DEFAULT 0,
    rank_order INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_snapshot_time (snapshot_time),
    INDEX idx_snapshot_rank (snapshot_time, rank_order),
    INDEX idx_author_id (author_id)
);

CREATE TABLE IF NOT EXISTS video_appeals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    appeal_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    reviewer_id BIGINT,
    review_comment TEXT,
    review_result VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES videos(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_video_id (video_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);
