package org.hackathon.finlabvalidator.tests;

import org.hackathon.finlabvalidator.api.models.CreateSessionRequest;
import org.hackathon.finlabvalidator.api.models.EndSessionRequest;
import org.hackathon.finlabvalidator.persistence.domain.AuthSession;
import org.hackathon.finlabvalidator.persistence.domain.SessionStatus;
import org.hackathon.finlabvalidator.persistence.repository.AuthSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthSessionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Value("${security.api-key}")
    private String apiKey;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/auth-sessions";
    }

    @BeforeEach
    void setUp() {
        authSessionRepository.deleteAll();
    }

    @Test
    void createSession_WithValidRequest_ShouldCreateSessionInDatabase() {
        String token = "test-jwt-token-123";
        String username = "testuser";
        CreateSessionRequest request = new CreateSessionRequest(token, username);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", apiKey);
        HttpEntity<CreateSessionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl(), entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authSessionRepository.count()).isEqualTo(1);

        AuthSession session = authSessionRepository.findAll().get(0);
        assertThat(session.getUsername()).isEqualTo(username);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getLoginTime()).isNotNull();
        assertThat(session.getLogoutTime()).isNull();
    }

    @Test
    void createSession_WithoutApiKey_ShouldReturnUnauthorized() {
        CreateSessionRequest request = new CreateSessionRequest("token", "user");

        HttpEntity<CreateSessionRequest> entity = new HttpEntity<>(request);

        try {
            restTemplate.exchange(
                    baseUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            assertThat(false).as("Expected 401 response but got success").isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            assertThat(e.getCause()).isInstanceOf(java.net.HttpRetryException.class);
            assertThat(e.getMessage()).contains("cannot retry due to server authentication");
        }
    }

    @Test
    void endSession_WithValidToken_ShouldUpdateSessionStatus() {
        String token = "test-jwt-token-456";
        CreateSessionRequest createRequest = new CreateSessionRequest(token, "testuser");

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", apiKey);
        HttpEntity<CreateSessionRequest> createEntity = new HttpEntity<>(createRequest, headers);
        restTemplate.postForEntity(baseUrl(), createEntity, String.class);

        EndSessionRequest endRequest = new EndSessionRequest(token);
        HttpEntity<EndSessionRequest> endEntity = new HttpEntity<>(endRequest, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/end",
                HttpMethod.PUT,
                endEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        AuthSession session = authSessionRepository.findAll().get(0);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.LOGGED_OUT);
        assertThat(session.getLogoutTime()).isNotNull();
    }

    @Test
    void endSession_WithNonExistentToken_ShouldStillReturnNoContent() {
        EndSessionRequest request = new EndSessionRequest("non-existent-token");

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", apiKey);
        HttpEntity<EndSessionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/end",
                HttpMethod.PUT,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void endSession_WithoutApiKey_ShouldReturnUnauthorized() {
        EndSessionRequest request = new EndSessionRequest("token");

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/end",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
