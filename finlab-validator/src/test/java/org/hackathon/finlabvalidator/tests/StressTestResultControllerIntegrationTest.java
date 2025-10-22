package org.hackathon.finlabvalidator.tests;

import org.hackathon.finlabvalidator.persistence.domain.TestResultListItem;
import org.hackathon.finlabvalidator.persistence.domain.TestResultSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "stress-tests.path=${java.io.tmpdir}/stress_tests_test"
})
public class StressTestResultControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @TempDir
    Path tempDir;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/results";
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", "test-api-key");
        return headers;
    }

    @BeforeEach
    void setUp() throws IOException {
        Path stressTestsDir = Path.of(System.getProperty("java.io.tmpdir"), "stress_tests_test");
        Files.createDirectories(stressTestsDir);

        String jtlContent = """
                timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
                1698765432000,245,GET Account Status,200,OK,Thread Group 1-1,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,240,0,10
                1698765432500,312,GET Account Status,200,OK,Thread Group 1-2,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,305,0,12
                1698765433000,189,GET Account Status,200,OK,Thread Group 1-3,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,185,0,8
                1698765433500,456,GET Account Status,200,OK,Thread Group 1-4,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,450,0,15
                1698765434000,523,GET Account Status,500,Internal Server Error,Thread Group 1-5,text,false,Connection timeout,512,512,50,50,https://localhost:443/api/v1/accounts/TEST,520,0,20
                1698765434500,298,GET Account Status,200,OK,Thread Group 1-6,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,295,0,11
                1698765435000,201,GET Account Status,200,OK,Thread Group 1-7,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,198,0,9
                1698765435500,367,GET Account Status,200,OK,Thread Group 1-8,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,362,0,13
                1698765436000,412,GET Account Status,200,OK,Thread Group 1-9,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,408,0,14
                1698765436500,278,GET Account Status,200,OK,Thread Group 1-10,text,true,,1024,512,50,50,https://localhost:443/api/v1/accounts/TEST,275,0,10
                """;

        Files.writeString(stressTestsDir.resolve("test-load-results.jtl"), jtlContent);
    }

    @Test
    void testListTestResults_ReturnsAvailableTests() {
        HttpEntity<Void> entity = new HttpEntity<>(headers());

        ResponseEntity<List<TestResultListItem>> response = restTemplate.exchange(
                baseUrl(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        TestResultListItem item = response.getBody().get(0);
        assertThat(item.testId()).isEqualTo("test-load");
        assertThat(item.testName()).isEqualTo("Test Load");
        assertThat(item.fileName()).isEqualTo("test-load-results.jtl");
    }

    @Test
    void testGetTestResultSummary_ValidTestId_ReturnsMetrics() {
        HttpEntity<Void> entity = new HttpEntity<>(headers());

        ResponseEntity<TestResultSummary> response = restTemplate.exchange(
                baseUrl() + "/test-load",
                HttpMethod.GET,
                entity,
                TestResultSummary.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TestResultSummary summary = response.getBody();
        assertThat(summary.testId()).isEqualTo("test-load");
        assertThat(summary.testName()).isEqualTo("Test Load");
        assertThat(summary.totalRequests()).isEqualTo(10);
        assertThat(summary.successfulRequests()).isEqualTo(9);
        assertThat(summary.failedRequests()).isEqualTo(1);
        assertThat(summary.errorRate()).isEqualTo(10.0);
        assertThat(summary.minResponseTime()).isEqualTo(189);
        assertThat(summary.maxResponseTime()).isEqualTo(523);
        assertThat(summary.averageResponseTime()).isGreaterThan(0);
        assertThat(summary.p90ResponseTime()).isGreaterThan(0);
        assertThat(summary.p95ResponseTime()).isGreaterThan(0);
    }

    @Test
    void testGetTestResultSummary_InvalidTestId_ReturnsNotFound() {
        HttpEntity<Void> entity = new HttpEntity<>(headers());

        ResponseEntity<TestResultSummary> response = restTemplate.exchange(
                baseUrl() + "/non-existent-test",
                HttpMethod.GET,
                entity,
                TestResultSummary.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testListTestResults_MissingApiKey_ReturnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl(),
                HttpMethod.GET,
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
