package com.rush.ticket.reservation.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;

    // Порівнюємо токен перед видаленням — інакше можна випадково зняти
    // чужий лок, якщо наш уже протух і хтось встиг захопити місце заново.
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """, Long.class);

    public SeatLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String eventId, String seatId, String token, Duration ttl) {
        String key = lockKey(eventId, seatId);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void unlock(String eventId, String seatId, String token) {
        String key = lockKey(eventId, seatId);
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
    }

    private String lockKey(String eventId, String seatId) {
        return "seat-lock:%s:%s".formatted(eventId, seatId);
    }
}