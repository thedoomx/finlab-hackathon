package org.hackathon.finlabvalidator.application;

import org.hackathon.finlabvalidator.persistence.domain.TestResultListItem;
import org.hackathon.finlabvalidator.persistence.domain.TestResultSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StressTestResultService implements IStressTestResultService {

    private static final String SEPARATOR = ".";

    private final Path stressTestsPath;

    public StressTestResultService(@Value("${stress-tests.path:../stress_tests}") String stressTestsPath) {
        this.stressTestsPath = Paths.get(stressTestsPath);
    }

    @Override
    public List<TestResultListItem> list() {
        try (Stream<Path> paths = Files.walk(stressTestsPath, 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("-results.jtl"))
                    .map(this::toTestResultListItem)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(Comparator.comparing(TestResultListItem::executionDate).reversed())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private Optional<TestResultListItem> toTestResultListItem(Path path) {
        try {
            String fileName = path.getFileName().toString();
            String testId = fileName.replace("-results.jtl", "");
            String testName = formatTestName(testId);

            LocalDateTime executionDate = getExecutionDateFromJtl(path);

            return Optional.of(new TestResultListItem(testId, testName, executionDate, fileName));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private LocalDateTime getExecutionDateFromJtl(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            reader.readLine();
            String firstDataLine = reader.readLine();
            if (firstDataLine != null) {
                String[] fields = firstDataLine.split(",");
                if (fields.length > 0) {
                    long timestamp = Long.parseLong(fields[0]);
                    return LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            ZoneId.systemDefault()
                    );
                }
            }
        } catch (Exception e) {
        }
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(attrs.lastModifiedTime().toMillis()),
                ZoneId.systemDefault()
        );
    }

    @Override
    public Optional<TestResultSummary> getSummary(String testId) {
        Path filePath = stressTestsPath.resolve(testId + "-results.jtl");

        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(parseJtlFile(filePath, testId));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private TestResultSummary parseJtlFile(Path filePath, String testId) throws IOException {
        List<Long> responseTimes = new ArrayList<>();
        long successCount = 0;
        long failCount = 0;
        LocalDateTime executionDate = getExecutionDateFromJtl(filePath);

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length < 8) continue;

                try {
                    long elapsed = Long.parseLong(fields[1]);
                    boolean success = "true".equalsIgnoreCase(fields[7]);

                    responseTimes.add(elapsed);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                }
            }
        }

        long totalRequests = successCount + failCount;
        double errorRate = totalRequests > 0 ? (failCount * 100.0 / totalRequests) : 0.0;

        Collections.sort(responseTimes);

        long avgResponseTime = responseTimes.isEmpty() ? 0 :
                (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long minResponseTime = responseTimes.isEmpty() ? 0 : responseTimes.get(0);
        long maxResponseTime = responseTimes.isEmpty() ? 0 : responseTimes.get(responseTimes.size() - 1);
        long p90ResponseTime = getPercentile(responseTimes, 90);
        long p95ResponseTime = getPercentile(responseTimes, 95);

        double throughput = 0.0;

        return new TestResultSummary(
                testId,
                formatTestName(testId),
                executionDate,
                totalRequests,
                successCount,
                failCount,
                Math.round(errorRate * 100.0) / 100.0,
                avgResponseTime,
                minResponseTime,
                maxResponseTime,
                p90ResponseTime,
                p95ResponseTime,
                throughput
        );
    }

    private long getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private String formatTestName(String testId) {
        if (testId.matches(".*-\\d{8}-\\d{6}$")) {
            String[] parts = testId.split("-");
            int timestampStartIndex = parts.length - 2;

            String baseName = Arrays.stream(parts)
                    .limit(timestampStartIndex)
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                    .collect(Collectors.joining(" "));

            String dateStr = parts[timestampStartIndex];
            String formattedDate = dateStr.substring(0, 4) + SEPARATOR +
                                   dateStr.substring(4, 6) + SEPARATOR +
                                   dateStr.substring(6, 8);

            String timeStr = parts[timestampStartIndex + 1];
            String formattedTime = timeStr.substring(0, 2) + SEPARATOR +
                                   timeStr.substring(2, 4) + SEPARATOR +
                                   timeStr.substring(4, 6);

            return baseName + " " + formattedDate + "_" + formattedTime;
        }

        return Arrays.stream(testId.split("-"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
