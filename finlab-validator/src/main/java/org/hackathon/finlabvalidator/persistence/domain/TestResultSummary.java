package org.hackathon.finlabvalidator.persistence.domain;

import java.time.LocalDateTime;

public record TestResultSummary(
        String testId,
        String testName,
        LocalDateTime executionDate,
        Long totalRequests,
        Long successfulRequests,
        Long failedRequests,
        Double errorRate,
        Long averageResponseTime,
        Long minResponseTime,
        Long maxResponseTime,
        Long p90ResponseTime,
        Long p95ResponseTime,
        Double throughput
) {
}
