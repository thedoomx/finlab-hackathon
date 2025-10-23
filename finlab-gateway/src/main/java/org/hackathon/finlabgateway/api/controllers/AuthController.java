package org.hackathon.finlabgateway.api.controllers;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.hackathon.finlabgateway.api.models.LoginRequest;
import org.hackathon.finlabgateway.infrastructure.AuthSessionClient;
import org.hackathon.finlabgateway.infrastructure.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TokenService tokenService;
    private final AuthSessionClient authSessionClient;
    private final SecretKey jwtKey;
    private final long jwtExpirationMs;

    public AuthController(
            TokenService tokenService,
            AuthSessionClient authSessionClient,
            @Value("${security.jwt.secret}") String jwtSecret,
            @Value("${security.jwt.ttl}") Duration jwtTtl
    ) {
        this.tokenService = tokenService;
        this.authSessionClient = authSessionClient;
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtTtl.toMillis();
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestBody LoginRequest loginRequest) {

        String token = Jwts.builder()
                .setSubject(loginRequest.username())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();

        return tokenService.store(loginRequest.username(), token)
                .then(authSessionClient.createSession(token, loginRequest.username()))
                .thenReturn(ResponseEntity.ok(token));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String token = authHeader.substring("Bearer ".length());
        String username = Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        return tokenService.remove(username)
                .then(authSessionClient.endSession(token))
                .thenReturn(ResponseEntity.noContent().build());
    }
}
