package org.hackathon.finlabvalidator.application;

import org.hackathon.finlabvalidator.persistence.domain.TestResultListItem;
import org.hackathon.finlabvalidator.persistence.domain.TestResultSummary;

import java.util.List;
import java.util.Optional;

public interface IStressTestResultService {
    List<TestResultListItem> list();
    Optional<TestResultSummary> getSummary(String testId);
}
