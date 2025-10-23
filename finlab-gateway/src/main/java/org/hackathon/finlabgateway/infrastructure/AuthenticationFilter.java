package org.hackathon.finlabgateway.infrastructure;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.hackathon.finlabgateway.common.AuthConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Order(1)
public class AuthenticationFilter implements GlobalFilter {

    private final String expectedApiKey;
    private final SecretKey jwtKey;
    private final TokenService tokenService;

    public AuthenticationFilter(
            @Value("${security.api-key}") String expectedApiKey,
            @Value("${security.jwt.secret}") String jwtSecret,
            TokenService tokenService
    ) {
        this.expectedApiKey = expectedApiKey;
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenService = tokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (shouldBypassAuthentication(path)) {
            return chain.filter(exchange);
        }

        if (!isApiKeyValid(exchange)) {
            return unauthorized(exchange);
        }

        String token = extractBearerToken(exchange);
        if (token == null) {
            return unauthorized(exchange);
        }

        String username = parseJwtToken(token);
        if (username == null) {
            return unauthorized(exchange);
        }

        return tokenService.isValid(username, token)
                .flatMap(valid -> valid ? chain.filter(exchange) : unauthorized(exchange));
    }

    private boolean shouldBypassAuthentication(String path) {
        return path.startsWith("/auth/") || path.startsWith("/actuator/health");
    }

    private boolean isApiKeyValid(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(AuthConstants.API_KEY_HEADER);
        return apiKey != null && apiKey.equals(expectedApiKey);
    }

    private String extractBearerToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(AuthConstants.AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(AuthConstants.BEARER_PREFIX.length());
    }

    private String parseJwtToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(jwtKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}