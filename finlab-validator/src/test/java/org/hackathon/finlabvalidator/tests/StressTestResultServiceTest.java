package org.hackathon.finlabvalidator.tests;

import org.hackathon.finlabvalidator.application.StressTestResultService;
import org.hackathon.finlabvalidator.persistence.domain.TestResultListItem;
import org.hackathon.finlabvalidator.persistence.domain.TestResultSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class StressTestResultServiceTest {

    @TempDir
    Path tempDir;

    private StressTestResultService service;

    @BeforeEach
    void setUp() {
        service = new StressTestResultService(tempDir.toString());
    }

    @Test
    void list_ShouldReturnEmptyList_WhenNoJtlFilesExist() {
        List<TestResultListItem> results = service.list();

        assertThat(results).isEmpty();
    }

    @Test
    void list_ShouldReturnTestResults_WhenJtlFilesExist() throws IOException {
        createSampleJtlFile("normal-load-results.jtl");
        createSampleJtlFile("high-load-results.jtl");

        List<TestResultListItem> results = service.list();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(TestResultListItem::testId)
                .containsExactlyInAnyOrder("normal-load", "high-load");
    }

    @Test
    void list_ShouldFormatTestName_WithTimestamp() throws IOException {
        createSampleJtlFile("normal-load-20251023-103045-results.jtl");

        List<TestResultListItem> results = service.list();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).testName()).isEqualTo("Normal Load 2025.10.23_10.30.45");
    }

    @Test
    void list_ShouldFormatTestName_WithoutTimestamp() throws IOException {
        createSampleJtlFile("normal-load-results.jtl");

        List<TestResultListItem> results = service.list();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).testName()).isEqualTo("Normal Load");
    }

    @Test
    void list_ShouldSortByExecutionDate_Descending() throws IOException, InterruptedException {
        createSampleJtlFile("test1-results.jtl", 1730000000000L);
        Thread.sleep(10);
        createSampleJtlFile("test2-results.jtl", 1730000100000L);

        List<TestResultListItem> results = service.list();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).testId()).isEqualTo("test2");
        assertThat(results.get(1).testId()).isEqualTo("test1");
    }

    @Test
    void getSummary_ShouldReturnEmpty_WhenFileDoesNotExist() {
        Optional<TestResultSummary> summary = service.getSummary("non-existent");

        assertThat(summary).isEmpty();
    }

    @Test
    void getSummary_ShouldReturnSummary_WithCorrectMetrics() throws IOException {
        String testId = "normal-load";
        createSampleJtlFile(testId + "-results.jtl");

        Optional<TestResultSummary> summary = service.getSummary(testId);

        assertThat(summary).isPresent();
        TestResultSummary result = summary.get();

        assertThat(result.testId()).isEqualTo(testId);
        assertThat(result.testName()).isEqualTo("Normal Load");
        assertThat(result.totalRequests()).isEqualTo(3);
        assertThat(result.successfulRequests()).isEqualTo(2);
        assertThat(result.failedRequests()).isEqualTo(1);
        assertThat(result.errorRate()).isEqualTo(33.33);
        assertThat(result.averageResponseTime()).isEqualTo(166);
        assertThat(result.minResponseTime()).isEqualTo(100);
        assertThat(result.maxResponseTime()).isEqualTo(250);
    }

    @Test
    void getSummary_ShouldCalculatePercentiles_Correctly() throws IOException {
        String testId = "percentile-test";
        createLargeJtlFile(testId + "-results.jtl");

        Optional<TestResultSummary> summary = service.getSummary(testId);

        assertThat(summary).isPresent();
        TestResultSummary result = summary.get();

        assertThat(result.p90ResponseTime()).isGreaterThan(0);
        assertThat(result.p95ResponseTime()).isGreaterThan(0);
        assertThat(result.p95ResponseTime()).isGreaterThanOrEqualTo(result.p90ResponseTime());
    }

    @Test
    void getSummary_ShouldHandleEmptyFile_Gracefully() throws IOException {
        String testId = "empty-test";
        createEmptyJtlFile(testId + "-results.jtl");

        Optional<TestResultSummary> summary = service.getSummary(testId);

        assertThat(summary).isPresent();
        TestResultSummary result = summary.get();

        assertThat(result.totalRequests()).isEqualTo(0);
        assertThat(result.averageResponseTime()).isEqualTo(0);
        assertThat(result.minResponseTime()).isEqualTo(0);
        assertThat(result.maxResponseTime()).isEqualTo(0);
    }

    @Test
    void getSummary_ShouldSkipMalformedLines() throws IOException {
        String testId = "malformed-test";
        createMalformedJtlFile(testId + "-results.jtl");

        Optional<TestResultSummary> summary = service.getSummary(testId);

        assertThat(summary).isPresent();
        TestResultSummary result = summary.get();

        assertThat(result.totalRequests()).isEqualTo(2);
        assertThat(result.successfulRequests()).isEqualTo(2);
    }

    private void createSampleJtlFile(String fileName) throws IOException {
        createSampleJtlFile(fileName, System.currentTimeMillis());
    }

    private void createSampleJtlFile(String fileName, long timestamp) throws IOException {
        String content = """
                timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
                %d,100,/api/v1/accounts,200,OK,Thread-1,text,true,,1024,256,1,1,https://localhost/api/v1/accounts,50,0,10
                %d,150,/api/v1/accounts,200,OK,Thread-2,text,true,,1024,256,1,1,https://localhost/api/v1/accounts,75,0,15
                %d,250,/api/v1/accounts,500,Error,Thread-3,text,false,Connection timeout,512,256,1,1,https://localhost/api/v1/accounts,200,0,20
                """.formatted(timestamp, timestamp + 100, timestamp + 200);

        Files.writeString(tempDir.resolve(fileName), content);
    }

    private void createLargeJtlFile(String fileName) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect\n");

        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            long elapsed = 100 + (i * 10);
            content.append(String.format("%d,%d,/api/v1/accounts,200,OK,Thread-%d,text,true,,1024,256,1,1,https://localhost/api/v1/accounts,50,0,10\n",
                    timestamp + i, elapsed, i));
        }

        Files.writeString(tempDir.resolve(fileName), content.toString());
    }

    private void createEmptyJtlFile(String fileName) throws IOException {
        String content = "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect\n";
        Files.writeString(tempDir.resolve(fileName), content);
    }

    private void createMalformedJtlFile(String fileName) throws IOException {
        long timestamp = System.currentTimeMillis();
        String content = """
                timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
                %d,100,/api/v1/accounts,200,OK,Thread-1,text,true,,1024,256,1,1,https://localhost/api/v1/accounts,50,0,10
                malformed,line,with,invalid,data
                invalid,data
                %d,150,/api/v1/accounts,200,OK,Thread-2,text,true,,1024,256,1,1,https://localhost/api/v1/accounts,75,0,15
                """.formatted(timestamp, timestamp + 100);

        Files.writeString(tempDir.resolve(fileName), content);
    }
}
