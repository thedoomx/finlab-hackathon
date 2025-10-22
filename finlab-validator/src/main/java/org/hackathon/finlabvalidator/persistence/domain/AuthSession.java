package org.hackathon.finlabvalidator.persistence.domain;

import java.time.LocalDateTime;

public class AuthSession {

    private Long id;
    private String tokenHash;
    private String username;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private SessionStatus status;
    private LocalDateTime createdAt;

    public AuthSession() {
    }

    public AuthSession(Long id, String tokenHash, String username, LocalDateTime loginTime,
                       LocalDateTime logoutTime, SessionStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.username = username;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
