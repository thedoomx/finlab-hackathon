package org.hackathon.finlabvalidator.persistence.domain;

import java.time.LocalDateTime;

public record TestResultListItem(
        String testId,
        String testName,
        LocalDateTime executionDate,
        String fileName
) {
}
