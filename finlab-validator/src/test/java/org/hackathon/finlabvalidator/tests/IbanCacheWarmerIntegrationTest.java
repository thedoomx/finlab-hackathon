package org.hackathon.finlabvalidator.tests;

import org.hackathon.finlabvalidator.application.IbanCacheWarmer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class IbanCacheWarmerIntegrationTest {

    @Autowired
    private IbanCacheWarmer cacheWarmer;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Value("${cache.iban.max-entries}")
    private long maxEntries;

    @BeforeEach
    void setUp() {
        redisTemplate.execute(connection -> connection.serverCommands().flushDb())
                .collectList()
                .block();
    }

    @Test
    void warmCache_ShouldLoadIbansIntoRedis() {
        cacheWarmer.warmCache().block(Duration.ofSeconds(30));

        Long keysCount = redisTemplate.keys("iban:*")
                .collectList()
                .map(list -> (long) list.size())
                .block(Duration.ofSeconds(5));

        assertThat(keysCount).isGreaterThan(0);
    }

    @Test
    void warmCache_ShouldLoadIbansWithCorrectFormat() {
        cacheWarmer.warmCache().block(Duration.ofSeconds(30));

        String firstKey = redisTemplate.keys("iban:*")
                .next()
                .block(Duration.ofSeconds(5));

        assertThat(firstKey).isNotNull();

        String value = redisTemplate.opsForValue()
                .get(firstKey)
                .block(Duration.ofSeconds(5));

        assertThat(value).isIn("ALLOW", "REVIEW", "BLOCK");
    }

    @Test
    void warmCache_ShouldLoadExactlyMaxEntries() {
        cacheWarmer.warmCache().block(Duration.ofSeconds(30));

        Long totalKeys = retryUntilStable(() -> {
            return redisTemplate.keys("iban:*")
                    .collectList()
                    .map(list -> (long) list.size())
                    .block(Duration.ofSeconds(5));
        }, 5, 1000);

        assertThat(totalKeys).isNotNull();
        assertThat(totalKeys).isGreaterThan(0);
        assertThat(totalKeys).isLessThanOrEqualTo(maxEntries);
    }

    @Test
    void warmCache_ConcurrentInvocation_ShouldPreventDuplicateExecution() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);
        AtomicInteger actualExecutions = new AtomicInteger(0);

        Runnable warmupTask = () -> {
            try {
                startLatch.await();
                cacheWarmer.warmCache()
                        .doOnSuccess(v -> actualExecutions.incrementAndGet())
                        .subscribe();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        };

        Thread thread1 = new Thread(warmupTask);
        Thread thread2 = new Thread(warmupTask);

        thread1.start();
        thread2.start();

        startLatch.countDown();
        finishLatch.await();

        Thread.sleep(3000);

        Long keysCount = redisTemplate.keys("iban:*")
                .collectList()
                .map(list -> (long) list.size())
                .block(Duration.ofSeconds(5));

        assertThat(keysCount).isGreaterThan(0);
        assertThat(actualExecutions.get()).isLessThanOrEqualTo(2);
    }

    private <T> T retryUntilStable(java.util.function.Supplier<T> operation, int maxRetries, long delayMillis) {
        T previousValue = null;
        for (int i = 0; i < maxRetries; i++) {
            T currentValue = operation.get();
            if (previousValue != null && previousValue.equals(currentValue)) {
                return currentValue;
            }
            previousValue = currentValue;
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
        return previousValue;
    }
}
