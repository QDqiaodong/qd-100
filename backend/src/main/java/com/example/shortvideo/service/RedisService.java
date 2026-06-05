package com.example.shortvideo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final long DEFAULT_EXPIRE = 600;
    private volatile boolean redisAvailable = true;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "redis-recovery");
        t.setDaemon(true);
        return t;
    });
    
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public boolean isAvailable() {
        return redisAvailable;
    }
    
    public void set(String key, Object value) {
        if (!redisAvailable) return;
        try {
            redisTemplate.opsForValue().set(key, value, DEFAULT_EXPIRE, TimeUnit.SECONDS);
        } catch (Exception e) {
            handleRedisError(e);
        }
    }
    
    public void set(String key, Object value, long expire) {
        if (!redisAvailable) return;
        try {
            redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            handleRedisError(e);
        }
    }
    
    public Object get(String key) {
        if (!redisAvailable) return null;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            handleRedisError(e);
            return null;
        }
    }
    
    public String getString(String key) {
        if (!redisAvailable) return null;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            handleRedisError(e);
            return null;
        }
    }
    
    public Long increment(String key) {
        if (!redisAvailable) return null;
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            handleRedisError(e);
            return null;
        }
    }
    
    public Long increment(String key, long delta) {
        if (!redisAvailable) return null;
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            handleRedisError(e);
            return null;
        }
    }
    
    public boolean exists(String key) {
        if (!redisAvailable) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            handleRedisError(e);
            return false;
        }
    }
    
    public void delete(String key) {
        if (!redisAvailable) return;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            handleRedisError(e);
        }
    }
    
    public void expire(String key, long seconds) {
        if (!redisAvailable) return;
        try {
            redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            handleRedisError(e);
        }
    }
    
    private void handleRedisError(Exception e) {
        log.warn("Redis operation failed, marking as unavailable: {}", e.getMessage());
        redisAvailable = false;
        scheduler.schedule(() -> {
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                redisAvailable = true;
                log.info("Redis connection restored");
            } catch (Exception ex) {
                log.warn("Redis recovery check failed, will retry: {}", ex.getMessage());
                scheduler.schedule(() -> {
                    redisAvailable = true;
                }, 30, TimeUnit.SECONDS);
            }
        }, 30, TimeUnit.SECONDS);
    }
}
