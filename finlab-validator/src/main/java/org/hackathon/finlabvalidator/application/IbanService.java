package org.hackathon.finlabvalidator.application;

import org.hackathon.finlabvalidator.persistence.domain.IbanStatus;
import org.hackathon.finlabvalidator.infrastructure.RedisConfig;
import org.hackathon.finlabvalidator.persistence.repository.IIbanRepository;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class IbanService implements IAccountService {
    private final IIbanRepository repository;
    private final ReactiveStringRedisTemplate redis;
    private final Duration cacheTtl;

    public IbanService(IIbanRepository repository,
                       ReactiveStringRedisTemplate redis,
                       RedisConfig redisConfig) {
        this.repository = repository;
        this.redis = redis;
        this.cacheTtl = redisConfig.getDefaultTTL();
    }

    @Override
    public Mono<IbanStatus> validate(String iban) {
        String cacheKey = cacheKey(iban);

        return redis.opsForValue().get(cacheKey)
                .map(IbanStatus::valueOf)
                .switchIfEmpty(
                        Mono.fromCallable(() -> repository.findByIban(iban))
                                .flatMap(optionalDto -> optionalDto
                                        .map(dto -> redis.opsForValue()
                                                .set(cacheKey, dto.status().name(), cacheTtl)
                                                .thenReturn(dto.status()))
                                        .orElse(Mono.empty())
                                )
                );
    }

    private static String cacheKey(String iban) {
        return "iban:" + iban;
    }
}
