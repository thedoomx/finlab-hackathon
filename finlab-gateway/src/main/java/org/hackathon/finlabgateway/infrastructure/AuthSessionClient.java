package org.hackathon.finlabgateway.infrastructure;

import org.hackathon.finlabgateway.api.models.CreateSessionRequest;
import org.hackathon.finlabgateway.api.models.EndSessionRequest;
import org.hackathon.finlabgateway.common.AuthConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AuthSessionClient {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionClient.class);

    private final WebClient webClient;
    private final String apiKey;

    public AuthSessionClient(
            @Value("${my.gateway.validator-url}") String validatorBaseUrl,
            @Value("${my.gateway.validator-api-key}") String apiKey,
            @Value("${app.api.version}") String apiVersion
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(validatorBaseUrl + "/api/v" + apiVersion + "/auth-sessions")
                .build();
        this.apiKey = apiKey;
    }

    public Mono<Void> createSession(String token, String username) {
        return webClient.post()
                .header(AuthConstants.API_KEY_HEADER, apiKey)
                .bodyValue(new CreateSessionRequest(token, username))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.debug("Failed to create session for user {}: {}", username, e.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<Void> endSession(String token) {
        return webClient.put()
                .uri("/end")
                .header(AuthConstants.API_KEY_HEADER, apiKey)
                .bodyValue(new EndSessionRequest(token))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.debug("Failed to end session: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
