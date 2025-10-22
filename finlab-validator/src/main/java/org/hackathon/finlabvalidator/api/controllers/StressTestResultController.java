package org.hackathon.finlabvalidator.api.controllers;

import org.hackathon.finlabvalidator.persistence.domain.TestResultListItem;
import org.hackathon.finlabvalidator.persistence.domain.TestResultSummary;
import org.hackathon.finlabvalidator.application.IStressTestResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${app.api.base-path}/results")
public class StressTestResultController {

    private final IStressTestResultService service;

    public StressTestResultController(IStressTestResultService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<TestResultListItem> listTestResults() {
        return Flux.fromIterable(service.list());
    }

    @GetMapping("/{testId}")
    public Mono<ResponseEntity<TestResultSummary>> getTestResultSummary(@PathVariable String testId) {
        return Mono.fromCallable(() -> service.getSummary(testId))
                .map(optional -> optional
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build())
                );
    }
}
