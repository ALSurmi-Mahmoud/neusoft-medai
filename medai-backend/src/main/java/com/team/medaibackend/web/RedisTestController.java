package com.team.medaibackend.web;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisTestController {

    private final StringRedisTemplate redis;

    public RedisTestController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/api/redis/ping")
    public String ping() {
        redis.opsForValue().set("medai:ping", "pong");
        return redis.opsForValue().get("medai:ping");
    }
}

