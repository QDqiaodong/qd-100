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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
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
    tags_text TEXT,
    file_status VARCHAR(20) DEFAULT 'not_uploaded',
    status VARCHAR(20) DEFAULT 'draft',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id_status (user_id, status)
);

INSERT INTO users (username, email, password, bio, followers, following) VALUES 
('testuser', 'test@example.com', 'password', '这是一个测试用户', 100, 50);

INSERT INTO videos (user_id, title, description, cover_url, video_url, duration, status) VALUES 
(1, '美食打卡：红烧肉制作', '今天做了一道美味的红烧肉，分享给大家！', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=delicious%20braised%20pork%20food%20photography&image_size=square', 'https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4', 126, 'approved'),
(1, '旅行日记：云南大理', '美丽的大理风光，让人沉醉', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=beautiful%20dali%20yunnan%20landscape%20travel&image_size=square', 'https://www.w3schools.com/html/movie.mp4', 13, 'approved'),
(1, '健身打卡：每日一练', '坚持健身第30天，加油！', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=fitness%20workout%20gym%20exercise&image_size=square', 'https://www.w3schools.com/html/mov_bbb.mp4', 10, 'approved'),
(1, '学习分享：编程入门', '从零开始学编程', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=programming%20coding%20computer%20science&image_size=square', 'https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4', 126, 'approved'),
(1, '音乐翻唱：夜曲', '翻唱周杰伦的经典歌曲', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=music%20singing%20microphone%20performance&image_size=square', 'https://www.w3schools.com/html/mov_bbb.mp4', 10, 'approved');

INSERT INTO tags (name) VALUES ('美食'), ('旅行'), ('健身'), ('学习'), ('音乐');

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
