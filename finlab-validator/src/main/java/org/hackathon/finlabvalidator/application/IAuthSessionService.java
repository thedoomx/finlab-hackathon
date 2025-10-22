package org.hackathon.finlabvalidator.application;

import org.hackathon.finlabvalidator.persistence.domain.AuthSession;

public interface IAuthSessionService {
    AuthSession create(String token, String username);
    void end(String token);
}
