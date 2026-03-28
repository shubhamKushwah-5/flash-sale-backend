package com.shubham.flashsale.loadtest;


// Simulates concurrent purchase requests to test locking mechanism

import com.shubham.flashsale.dto.PurchaseRequest;
import com.shubham.flashsale.dto.PurchaseResponse;
import org.springframework.http.*;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadTestRunner {

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public LoadTestRunner(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    //Run load test with specified parameters
    // @param productId  Product to purchase
    // @param threadCount Number of concurrent threads
    //@param requestPerThread requests each thread makes
    //@param usePessimistic true = pessimistic locking , false = optimistic
    //@return Test results

    public LoadTestResult runTest(
            Long productId,
            int threadCount,
            int requestsPerThread,
            boolean usePessimistic) {

        String lockingType = usePessimistic ? "Pessimistic" : "Optimistic";
        String endpoint = usePessimistic ? "/api/orders/purchase-pessimistic" :"/api/orders/purchase-optimistic";

        int totalRequests = threadCount * requestsPerThread;
        LoadTestResult result = new LoadTestResult(
                lockingType + " Locking Test",
                totalRequests,
                threadCount
        );

        System.out.println("\n Starting Load Test: " + lockingType);
        System.out.println("    Threads: " + threadCount);
        System.out.println("    Requests per thread: " + requestsPerThread);
        System.out.println("    Total requests: " + totalRequests);
        System.out.println("    Target product ID: " + productId);

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // CountDownLatch ensures all threads start simultaneously
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // Submit tasks to thread pool
        for (int i = 0; i< threadCount; i++) {
            final int userId = 1000 + i;  //Unique user ID per thread

            executor.submit(() -> {
                try {
                    // wait for all threads to be ready
                    startLatch.await();

                    // Each thread makes multiple requests
                    for (int j = 0 ; j< requestsPerThread; j++){
                        makePurchaseRequest(
                                productId,
                                userId,
                                endpoint,
                                result
                        );
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }finally {
                    completionLatch.countDown();
                }
            });
        }

        try {
            // start all thread simultaneously
            System.out.println(" All threads ready. Starting ......");
            Thread.sleep(1000);

            result.setTestStartTime(System.currentTimeMillis());
            startLatch.countDown(); // Release all threads at once

            // Wait for all threads to complete
            completionLatch.await();
            result.setTestEndTime(System.currentTimeMillis());

            System.out.println("All threads completed");

        } catch (InterruptedException e ) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return result;
    }


    // Make a single purchase request and record results

    private void makePurchaseRequest(
            Long productId,
            int userId,
            String endpoint,
            LoadTestResult result) {

        long threadId = Thread.currentThread().getId();
        long startTime = System.currentTimeMillis();

        try {
            // Create request
            PurchaseRequest request = new PurchaseRequest(productId, (long) userId, 1 );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PurchaseRequest> entity = new HttpEntity<>(request, headers);

            // Send request
            ResponseEntity<PurchaseResponse> response = restTemplate.exchange(
                    baseUrl + endpoint,
                    HttpMethod.POST,
                    entity,
                    PurchaseResponse.class
            );

            int responseTime = (int) (System.currentTimeMillis() - startTime);

            // Record result
            PurchaseResponse body = response.getBody();
            if(body != null && body.isSuccess()) {
                result.recordSuccess(threadId, responseTime);
            } else {
                result.recordFailure(threadId, responseTime);
            }
        }  catch (Exception e) {
            // Request failed (likely stock exhausted)
            int responseTime = (int) (System.currentTimeMillis() - startTime);
            result.recordFailure(threadId, responseTime);
        }
    }

    // Main mehtod to run tests

    public static void main(String[] args) {
        String baseUrl = "http://localhost:8080";
        LoadTestRunner runner = new LoadTestRunner(baseUrl);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("FLASH SALE CONCURRENCY LOAD TEST");
        System.out.println("=".repeat(80));

        /// Test parameters
        Long productId = 8L; // this product id should have 500 stock
        int threadCount = 1000;
        int requestsPerThread = 2;// so total 2000 request for 500 stock

        try {
            System.in.read();
        } catch (Exception e) {
             // ignore
        }

        // Test 1: Pessimistic Locking
        LoadTestResult pessimisticResult = runner.runTest(
                productId,
                threadCount,
                requestsPerThread,
                true
        );
        pessimisticResult.printResults();

        // wait between tests
        System.out.println("\n Waiting 5 seconds before next test...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create new product fo second test
        System.out.println("\n Create a new product with ID = 2 and stock = 500 for optimistic test");
        System.out.println("Press Enter to continue...");
        try  {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }

        // Test 2 : Optimistic locking
        LoadTestResult optimisticResult = runner.runTest(
                9l,
                threadCount,
                requestsPerThread,
                false
        );
        optimisticResult.printResults();

        // Compare results
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPARISON: Pessimistic vs Optimistic Locking");
        System.out.println("=".repeat(80));
        System.out.println("\nPessimistic Locking:");
        System.out.println("  Success: " + pessimisticResult.getSuccessCount());
        System.out.println("  Failure: " + pessimisticResult.getFailureCount());

        System.out.println("\nOptimistic Locking:");
        System.out.println("  Success: " + optimisticResult.getSuccessCount());
        System.out.println("  Failure: " + optimisticResult.getFailureCount());

        System.out.println("\n" + "=".repeat(80));
    }
}
