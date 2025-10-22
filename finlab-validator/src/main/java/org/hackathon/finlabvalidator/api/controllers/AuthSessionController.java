package org.hackathon.finlabvalidator.api.controllers;

import org.hackathon.finlabvalidator.api.models.CreateSessionRequest;
import org.hackathon.finlabvalidator.api.models.EndSessionRequest;
import org.hackathon.finlabvalidator.api.models.SessionResponse;
import org.hackathon.finlabvalidator.application.IAuthSessionService;
import org.hackathon.finlabvalidator.persistence.domain.AuthSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.base-path}/auth-sessions")
public class AuthSessionController {

    private final IAuthSessionService authSessionService;

    public AuthSessionController(IAuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@RequestBody CreateSessionRequest request) {
        AuthSession session = authSessionService.create(request.token(), request.username());
        return ResponseEntity.ok(new SessionResponse(session.getId(), "Session created"));
    }

    @PutMapping("/end")
    public ResponseEntity<Void> endSession(@RequestBody EndSessionRequest request) {
        authSessionService.end(request.token());
        return ResponseEntity.noContent().build();
    }
}
