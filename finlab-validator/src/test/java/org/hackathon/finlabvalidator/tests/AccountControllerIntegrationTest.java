package org.hackathon.finlabvalidator.tests;

import org.hackathon.finlabvalidator.persistence.domain.IbanDto;
import org.hackathon.finlabvalidator.persistence.domain.IbanStatus;
import org.hackathon.finlabvalidator.persistence.repository.IbanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReactiveRedisTemplate<String, String> redis;

    @Autowired
    private IbanRepository ibanRepository;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/accounts/";
    }

    @BeforeEach
    void cleanUp() {
        redis.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
    }

    @Test
    void testValidateAccount_FirstCall_CachesStatusInRedis() {
        IbanDto testIban = ibanRepository.findFirstIban().orElseThrow();
        String cacheKey = "iban:" + testIban.iban();

        Boolean existsBeforeFirstCall = redis.hasKey(cacheKey).block();
        assertThat(existsBeforeFirstCall).isFalse();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", "test-api-key");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<IbanStatus> firstResponse = restTemplate.exchange(
                baseUrl() + testIban.iban(),
                HttpMethod.GET,
                entity,
                IbanStatus.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getBody()).isEqualTo(testIban.status());

        Boolean existsAfterFirstCall = redis.hasKey(cacheKey).block();
        assertThat(existsAfterFirstCall).isTrue();

        String cachedValueAfterFirst = redis.opsForValue().get(cacheKey).block();
        assertThat(cachedValueAfterFirst).isNotNull();
        assertThat(cachedValueAfterFirst).isEqualTo(testIban.status().name());

        ResponseEntity<IbanStatus> secondResponse = restTemplate.exchange(
                baseUrl() + testIban.iban(),
                HttpMethod.GET,
                entity,
                IbanStatus.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResponse.getBody()).isEqualTo(testIban.status());

        Boolean existsAfterSecondCall = redis.hasKey(cacheKey).block();
        assertThat(existsAfterSecondCall).isTrue();

        String cachedValueAfterSecond = redis.opsForValue().get(cacheKey).block();
        assertThat(cachedValueAfterSecond).isNotNull();
        assertThat(cachedValueAfterSecond).isEqualTo(cachedValueAfterFirst);
    }
}
