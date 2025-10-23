package org.hackathon.finlabvalidator.application;

import org.hackathon.finlabvalidator.persistence.domain.IbanDto;
import org.hackathon.finlabvalidator.persistence.domain.PaginatedResult;
import org.hackathon.finlabvalidator.persistence.repository.IIbanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class IbanCacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(IbanCacheWarmer.class);
    private static final int PAGE_SIZE = 10000;

    private final IIbanRepository repository;
    private final ReactiveStringRedisTemplate redis;
    private final Duration cacheTtl;
    private final long maxEntries;
    private final boolean warmupEnabled;

    public IbanCacheWarmer(IIbanRepository repository,
                           ReactiveStringRedisTemplate redis,
                           org.hackathon.finlabvalidator.infrastructure.RedisConfig redisConfig,
                           @Value("${cache.iban.max-entries}") long maxEntries,
                           @Value("${cache.iban.warmup.enabled:true}") boolean warmupEnabled) {
        this.repository = repository;
        this.redis = redis;
        this.cacheTtl = redisConfig.getDefaultTTL();
        this.maxEntries = maxEntries;
        this.warmupEnabled = warmupEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        if (!warmupEnabled) {
            log.info("IBAN cache warm-up is disabled");
            return;
        }
        log.info("Scheduling IBAN cache warm-up to run in background...");
        warmCache().subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).subscribe(
                v -> log.info("Background cache warm-up completed successfully"),
                e -> log.error("Background cache warm-up failed", e)
        );
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledWarmCache() {
        log.info("Starting scheduled IBAN cache refresh...");
        warmCache().subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).subscribe(
                v -> log.info("Scheduled cache refresh completed successfully"),
                e -> log.error("Scheduled cache refresh failed", e)
        );
    }

    public Mono<Void> warmCache() {
        return Mono.fromCallable(() -> repository.findAll(1, 1))
                .flatMapMany(firstPage -> {
                    long totalCount = Math.min(firstPage.totalCount(), maxEntries);
                    long totalPages = (long) Math.ceil((double) totalCount / PAGE_SIZE);

                    log.info("Loading {} IBANs in {} pages (page size: {}, max entries: {})",
                            totalCount, totalPages, PAGE_SIZE, maxEntries);

                    return Flux.range(1, (int) totalPages)
                            .concatMap(page -> Mono.fromCallable(() -> repository.findAll(page, PAGE_SIZE))
                                    .flatMapMany(result -> Flux.fromIterable(result.items()))
                                    .buffer(PAGE_SIZE)
                                    .flatMap(this::saveToRedis)
                                    .doOnComplete(() -> log.info("Loaded page {}/{}", page, totalPages))
                            );
                })
                .then()
                .doOnSuccess(v -> log.info("IBAN cache warm-up completed successfully"))
                .doOnError(e -> log.error("Error during IBAN cache warm-up", e));
    }

    private Mono<Void> saveToRedis(java.util.List<IbanDto> ibans) {
        return Flux.fromIterable(ibans)
                .flatMap(iban -> redis.opsForValue()
                        .set(cacheKey(iban.iban()), iban.status().name(), cacheTtl))
                .then();
    }

    private static String cacheKey(String iban) {
        return "iban:" + iban;
    }
}
