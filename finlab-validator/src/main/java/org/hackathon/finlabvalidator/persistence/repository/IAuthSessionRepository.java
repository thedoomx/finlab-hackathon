package org.hackathon.finlabvalidator.persistence.repository;

import org.hackathon.finlabvalidator.persistence.domain.AuthSession;

import java.util.List;
import java.util.Optional;

public interface IAuthSessionRepository {
    AuthSession save(AuthSession session);
    Optional<AuthSession> findByTokenHash(String tokenHash);
    void deleteAll();
    long count();
    List<AuthSession> findAll();
}
