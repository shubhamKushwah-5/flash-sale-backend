package com.shubham.flashsale.loadtest;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// stores results from load testing

public class LoadTestResult {

    private final String testName;
    private final int totalRequests;
    private final int threadCount;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    private final ConcurrentHashMap<Long, Integer> responseTimes = new ConcurrentHashMap<>();

    private long testStartTime;
    private long testEndTime;

    public LoadTestResult(String testName, int totalRequests, int threadCount) {
        this.testName = testName;
        this.totalRequests = totalRequests;
        this.threadCount = threadCount;
    }

    public void recordSuccess(Long threadId, int responseTimeMs) {
        successCount.incrementAndGet();
        responseTimes.put(threadId, responseTimeMs);
    }

    public void recordFailure(long threadId, int responseTimeMs) {
        failureCount.incrementAndGet();
        responseTimes.put(threadId, responseTimeMs);
    }

    public void setTestStartTime(long startTime){
        this.testStartTime = startTime;
    }

    public void setTestEndTime(long endTime) {
        this.testEndTime = endTime;
    }

    //prints detailed results
    public void printResults() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("LOAD TEST RESULTS: " + testName);
        System.out.println("=".repeat(80));

        System.out.println("\nTest Configuration:");
        System.out.println("   Total Requests: " + totalRequests);
        System.out.println("   Total count: " + threadCount);
        System.out.println("   Total Duration " + (testEndTime - testStartTime) + "ms");

        System.out.println("\nResults:");
        System.out.println("  ✓ Successful Purchase: " + successCount.get());
        System.out.println("  ✗ Failed Purchase: " + failureCount.get());
        System.out.println("  Total Processed: " + (successCount.get() + failureCount.get()));


        // Calculate response time statistics
        List<Integer> times = responseTimes.values().stream().sorted().toList();
        if(!times.isEmpty()) {
            int min = times.get(0);
            int max = times.get(times.size() - 1);
            double avg = times.stream().mapToInt(Integer::intValue).average().orElse(0);
            int median = times.get(times.size() / 2);
            int p95 = times.get((int) (times.size() * 0.95));
            int p99 = times.get((int) (times.size() * 0.99));

            System.out.println("\nResponse Times (ms):");
            System.out.println(" Min: " + min);
            System.out.println(" Max: " + max);
            System.out.println(" Avg: " + String.format("%.2f", avg));
            System.out.println(" Median (p50): " + median);
            System.out.println(" P95: " + p95);
            System.out.println(" P99: " + p99);

        }

        System.out.println("\nThroughput:");
        double duration = (this.testEndTime - this.testStartTime) / 1000.0; // seconds
        double requestsPerSecond = totalRequests / duration;
        System.out.println("  Requests/Second: " + String.format("%.2f", requestsPerSecond));

        System.out.println("\n" + "=".repeat(80));
    }

    // Getters
    public int getSuccessCount() { return successCount.get();}
    public int getFailureCount() { return failureCount.get();}

}
