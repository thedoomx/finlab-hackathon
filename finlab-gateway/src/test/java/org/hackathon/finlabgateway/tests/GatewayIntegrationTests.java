package org.hackathon.finlabgateway.tests;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hackathon.finlabgateway.common.AuthConstants;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hackathon.finlabgateway.infrastructure.TokenService.TOKEN_PREFIX;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTests {

    static MockWebServer mockService;

    @LocalServerPort
    private int port;

    private static final String TEST_API_KEY = "finlab-validator-test-api-key";
    private static final String TEST_JWT_SECRET = "12345678901234567890123456789012";
    private static final String API_PREFIX_V1 = "/api/v1/";

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @BeforeAll
    static void setupMockServer() throws IOException {
        mockService = new MockWebServer();
        mockService.start();
    }

    @AfterAll
    static void shutdownMockServer() throws IOException {
        mockService.shutdown();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("security.api-key", () -> TEST_API_KEY);
        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("VALIDATOR_BASE_URL", () -> "http://localhost:" + mockService.getPort());
        registry.add("VALIDATOR_API_KEY", () -> TEST_API_KEY);
        registry.add("app.api.version", () -> "1");
    }

    private String gatewayUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String generateJwt(String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject("test-user")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private ResponseEntity<String> sendRequest(HttpHeaders headers, String path) {
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(gatewayUrl(path), HttpMethod.GET, entity, String.class);
    }

    private HttpHeaders authHeaders(String apiKey, String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null) headers.add(AuthConstants.API_KEY_HEADER, apiKey);
        if (jwtToken != null) headers.add(AuthConstants.AUTH_HEADER, AuthConstants.BEARER_PREFIX + jwtToken);
        return headers;
    }

    @Test
    void testAuthFilter_MissingApiKey_ReturnsUnauthorized() {
        String jwt = generateJwt(TEST_JWT_SECRET);
        ResponseEntity<String> response = sendRequest(authHeaders(null, jwt), API_PREFIX_V1 + "accounts/BG80BNBG96611020345678");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthFilter_MissingJwt_ReturnsUnauthorized() {
        ResponseEntity<String> response = sendRequest(authHeaders(TEST_API_KEY, null), API_PREFIX_V1 + "accounts/BG80BNBG96611020345678");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthFilter_InvalidJwtSignature_ReturnsUnauthorized() {
        String invalidJwt = generateJwt("55555678901234567890123456789012");
        ResponseEntity<String> response = sendRequest(authHeaders(TEST_API_KEY, invalidJwt), API_PREFIX_V1 + "accounts/BG80BNBG96611020345678");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testFullAuthFlow_Login_ThenAccess_ThenLogout_ThenAccessDenied() throws Exception {
        mockService.enqueue(new MockResponse()
                .setBody("{\"sessionId\":1,\"message\":\"Session created\"}")
                .addHeader("Content-Type", "application/json"));

        String loginUrl = gatewayUrl("/auth/login");
        Map<String, String> payload = Map.of("username", "test-user");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(loginUrl, payload, String.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody();
        assertThat(token).isNotBlank();

        assertThat(mockService.getRequestCount()).isEqualTo(1);
        var createSessionRequest = mockService.takeRequest();
        assertThat(createSessionRequest.getPath()).isEqualTo("/api/v1/auth-sessions");
        assertThat(createSessionRequest.getMethod()).isEqualTo("POST");

        mockService.enqueue(new MockResponse()
                .setBody("\"ALLOW\"")
                .addHeader("Content-Type", "application/json"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(AuthConstants.AUTH_HEADER, AuthConstants.BEARER_PREFIX + token);
        headers.add(AuthConstants.API_KEY_HEADER, TEST_API_KEY);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = gatewayUrl(API_PREFIX_V1 + "accounts/BG80BNBG96611020345678");
        ResponseEntity<String> response1 = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

        mockService.takeRequest();

        mockService.enqueue(new MockResponse()
                .setResponseCode(204));

        String logoutUrl = gatewayUrl("/auth/logout");
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(logoutUrl, HttpMethod.POST, entity, Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var endSessionRequest = mockService.takeRequest();
        assertThat(endSessionRequest.getPath()).isEqualTo("/api/v1/auth-sessions/end");
        assertThat(endSessionRequest.getMethod()).isEqualTo("PUT");

        ResponseEntity<String> response2 = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testLogin_ValidUsername_StoresTokenInRedis() {
        mockService.enqueue(new MockResponse()
                .setBody("{\"sessionId\":1,\"message\":\"Session created\"}")
                .addHeader("Content-Type", "application/json"));

        String loginUrl = gatewayUrl("/auth/login");
        Map<String, String> payload = Map.of("username", "test-user");

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(loginUrl, payload, String.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody();
        assertThat(token).isNotBlank();

        var redisKey = String.format("%stest-user", TOKEN_PREFIX);
        Boolean tokenExistsInRedis = redisTemplate.hasKey(redisKey).block();
        assertThat(tokenExistsInRedis).isTrue();

        String valueInRedis = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(valueInRedis).isEqualTo(token);
    }
}
