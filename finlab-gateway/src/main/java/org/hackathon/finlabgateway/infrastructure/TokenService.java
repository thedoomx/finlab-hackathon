package org.hackathon.finlabgateway.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class TokenService {

    public static final String TOKEN_PREFIX = "auth:token:";
    private final ReactiveStringRedisTemplate redisTemplate;
    private final Duration tokenTtl;

    public TokenService(ReactiveStringRedisTemplate redisTemplate,
                        @Value("${redis.auth-ttl}") Duration tokenTtl) {
        this.redisTemplate = redisTemplate;
        this.tokenTtl = tokenTtl;
    }

    public Mono<Void> store(String username, String token) {
        String key = TOKEN_PREFIX + username;
        return redisTemplate.opsForValue().set(key, token, tokenTtl).then();
    }

    public Mono<Boolean> isValid(String username, String token) {
        String key = TOKEN_PREFIX + username;
        return redisTemplate.opsForValue().get(key)
                .map(storedToken -> storedToken.equals(token))
                .defaultIfEmpty(false);
    }

    public Mono<Void> remove(String username) {
        String key = TOKEN_PREFIX + username;
        return redisTemplate.delete(key).then();
    }
}
