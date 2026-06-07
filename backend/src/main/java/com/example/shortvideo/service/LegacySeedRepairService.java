package com.example.shortvideo.service;

import com.example.shortvideo.entity.User;
import com.example.shortvideo.entity.Video;
import com.example.shortvideo.repository.UserRepository;
import com.example.shortvideo.repository.VideoRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LegacySeedRepairService {
    private static final Map<String, String> USER_BIO_REPAIRS = createUserBioRepairs();
    private static final Map<String, String> VIDEO_TITLE_REPAIRS = createVideoTitleRepairs();
    private static final Map<String, String> VIDEO_DESCRIPTION_REPAIRS = createVideoDescriptionRepairs();

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public LegacySeedRepairService(UserRepository userRepository, VideoRepository videoRepository) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void repairLegacySeedData() {
        for (User user : userRepository.findAll()) {
            String repairedBio = USER_BIO_REPAIRS.get(user.getBio());
            if (repairedBio != null) {
                user.setBio(repairedBio);
                userRepository.save(user);
            }
        }

        for (Video video : videoRepository.findAll()) {
            String repairedTitle = VIDEO_TITLE_REPAIRS.get(video.getTitle());
            String repairedDescription = VIDEO_DESCRIPTION_REPAIRS.get(video.getDescription());

            if (repairedTitle != null) {
                video.setTitle(repairedTitle);
            }
            if (repairedDescription != null) {
                video.setDescription(repairedDescription);
            }

            if (repairedTitle != null || repairedDescription != null) {
                videoRepository.save(video);
            }
        }
    }

    private static Map<String, String> createUserBioRepairs() {
        Map<String, String> repairs = new LinkedHashMap<>();
        repairs.put("è¿™æ˜¯ä¸€ä¸ªæµ‹è¯•ç”¨æˆ·", "这是一个测试用户");
        return repairs;
    }

    private static Map<String, String> createVideoTitleRepairs() {
        Map<String, String> repairs = new LinkedHashMap<>();
        repairs.put("ç¾Žé£Ÿæ‰“å¡ï¼šçº¢çƒ§è‚‰åˆ¶ä½œ", "美食打卡：红烧肉制作");
        repairs.put("æ—…è¡Œæ—¥è®°ï¼šäº‘å—å¤§ç†", "旅行日记：云南大理");
        repairs.put("å¥èº«æ‰“å¡ï¼šæ¯æ—¥ä¸€ç»ƒ", "健身打卡：每日一练");
        repairs.put("å­¦ä¹ åˆ†äº«ï¼šç¼–ç¨‹å…¥é—¨", "学习分享：编程入门");
        repairs.put("éŸ³ä¹ç¿»å”±ï¼šå¤œæ›²", "音乐翻唱：夜曲");
        return repairs;
    }

    private static Map<String, String> createVideoDescriptionRepairs() {
        Map<String, String> repairs = new LinkedHashMap<>();
        repairs.put("ä»Šå¤©åšäº†ä¸€é“ç¾Žå‘³çš„çº¢çƒ§è‚‰ï¼Œåˆ†äº«ç»™å¤§å®¶ï¼", "今天做了一道美味的红烧肉，分享给大家！");
        repairs.put("ç¾Žä¸½çš„å¤§ç†é£Žå…‰ï¼Œè®©äººæ²‰é†‰", "美丽的大理风光，让人沉醉");
        repairs.put("åšæŒå¥èº«ç¬¬30å¤©ï¼ŒåŠ æ²¹ï¼", "坚持健身第30天，加油！");
        repairs.put("ä»Žé›¶å¼€å§‹å­¦ç¼–ç¨‹", "从零开始学编程");
        repairs.put("ç¿»å”±å‘¨æ°ä¼¦çš„ç»å…¸æ­Œæ›²", "翻唱周杰伦的经典歌曲");
        return repairs;
    }
}
