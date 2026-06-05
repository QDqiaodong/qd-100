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
    status VARCHAR(20) DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
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

INSERT INTO users (username, email, password, bio, followers, following) VALUES 
('testuser', 'test@example.com', 'password', '这是一个测试用户', 100, 50);

INSERT INTO videos (user_id, title, description, cover_url, video_url, duration, status) VALUES 
(1, '美食打卡：红烧肉制作', '今天做了一道美味的红烧肉，分享给大家！', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=delicious%20braised%20pork%20food%20photography&image_size=square', 'https://www.w3schools.com/html/mov_bbb.mp4', 60, 'approved'),
(1, '旅行日记：云南大理', '美丽的大理风光，让人沉醉', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=beautiful%20dali%20yunnan%20landscape%20travel&image_size=square', 'https://www.w3schools.com/html/movie.mp4', 12, 'approved'),
(1, '健身打卡：每日一练', '坚持健身第30天，加油！', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=fitness%20workout%20gym%20exercise&image_size=square', 'https://www.w3schools.com/html/mov_bbb.mp4', 60, 'approved'),
(1, '学习分享：编程入门', '从零开始学编程', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=programming%20coding%20computer%20science&image_size=square', 'https://www.w3schools.com/html/movie.mp4', 12, 'approved'),
(1, '音乐翻唱：夜曲', '翻唱周杰伦的经典歌曲', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=music%20singing%20microphone%20performance&image_size=square', 'https://www.w3schools.com/html/mov_bbb.mp4', 60, 'approved');

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
(1, 2, 5, FALSE),
(1, 3, 30, FALSE);
