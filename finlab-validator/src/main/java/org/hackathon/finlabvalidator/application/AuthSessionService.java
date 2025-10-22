package org.hackathon.finlabvalidator.application;

import org.hackathon.finlabvalidator.persistence.domain.AuthSession;
import org.hackathon.finlabvalidator.persistence.domain.SessionStatus;
import org.hackathon.finlabvalidator.persistence.repository.IAuthSessionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class AuthSessionService implements IAuthSessionService {

    private final IAuthSessionRepository repository;

    public AuthSessionService(IAuthSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession create(String token, String username) {
        String tokenHash = hashToken(token);
        AuthSession session = new AuthSession();
        session.setTokenHash(tokenHash);
        session.setUsername(username);
        session.setLoginTime(LocalDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        return repository.save(session);
    }

    @Override
    public void end(String token) {
        String tokenHash = hashToken(token);
        repository.findByTokenHash(tokenHash).ifPresent(session -> {
            session.setLogoutTime(LocalDateTime.now());
            session.setStatus(SessionStatus.LOGGED_OUT);
            repository.save(session);
        });
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
