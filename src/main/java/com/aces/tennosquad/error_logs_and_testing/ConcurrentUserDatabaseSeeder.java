package com.aces.tennosquad.error_logs_and_testing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentUserDatabaseSeeder {

    private static final String API_URL =
            "http://localhost:8093/api/users";

    private static final int USER_COUNT = 50_000;

    // Number of requests that can run at the same time.
    // Try 25, 50, 100, 200, etc.
    private static final int CONCURRENCY = 100;

    private static final Path OUTPUT_FILE =
            Path.of("created-users.txt");

    private static final AtomicInteger successful = new AtomicInteger();
    private static final AtomicInteger failed = new AtomicInteger();
    private static final AtomicInteger completed = new AtomicInteger();

    public static void main(String[] args) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(CONCURRENCY);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(executor)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        BlockingQueue<String> credentialQueue =
                new LinkedBlockingQueue<>();

        Thread fileWriterThread = new Thread(() -> {
            writeCredentials(credentialQueue);
        });

        fileWriterThread.start();

        CompletableFuture<?>[] futures =
                new CompletableFuture[USER_COUNT];

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= USER_COUNT; i++) {

            String uniquePart =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 12);

            String userName =
                    "testuser_" + i + "_" + uniquePart;

            String warframeUserName =
                    "warframe_" + i + "_" + uniquePart;

            String password =
                    "TestPassword_" + uniquePart + "!";

            String json = """
                    {
                      "userName": "%s",
                      "warframeUserName": "%s",
                      "password": "%s"
                    }
                    """.formatted(
                    userName,
                    warframeUserName,
                    password
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(json)
                    )
                    .build();

            futures[i - 1] =
                    client.sendAsync(
                                    request,
                                    HttpResponse.BodyHandlers.ofString()
                            )
                            .thenAccept(response -> {

                                int statusCode =
                                        response.statusCode();

                                if (statusCode >= 200 &&
                                        statusCode < 300) {

                                    successful.incrementAndGet();

                                    credentialQueue.offer(
                                            userName
                                                    + ":"
                                                    + password
                                    );

                                } else {

                                    failed.incrementAndGet();

                                    System.err.printf(
                                            "FAILED %s -> HTTP %d: %s%n",
                                            userName,
                                            statusCode,
                                            response.body()
                                    );
                                }

                                printProgress();
                            })
                            .exceptionally(exception -> {

                                failed.incrementAndGet();

                                System.err.printf(
                                        "REQUEST ERROR %s: %s%n",
                                        userName,
                                        exception.getMessage()
                                );

                                printProgress();

                                return null;
                            });
        }

        // Wait for every request to finish
        CompletableFuture.allOf(futures).join();

        // Signal the file writer to stop
        credentialQueue.put("__STOP__");

        fileWriterThread.join();

        executor.shutdown();

        long elapsed =
                System.currentTimeMillis() - startTime;

        System.out.println();
        System.out.println("==============================");
        System.out.println("Finished");
        System.out.println("==============================");

        System.out.printf(
                "Successful: %,d%n",
                successful.get()
        );

        System.out.printf(
                "Failed:     %,d%n",
                failed.get()
        );

        System.out.printf(
                "Total time: %.2f seconds%n",
                elapsed / 1000.0
        );

        if (elapsed > 0) {
            double requestsPerSecond =
                    USER_COUNT / (elapsed / 1000.0);

            System.out.printf(
                    "Average:    %.2f requests/sec%n",
                    requestsPerSecond
            );
        }

        System.out.println(
                "Credentials: "
                        + OUTPUT_FILE.toAbsolutePath()
        );
    }

    private static void printProgress() {

        int count =
                completed.incrementAndGet();

        if (count % 100 == 0 ||
                count == USER_COUNT) {

            System.out.printf(
                    "Progress: %,d / %,d | Success: %,d | Failed: %,d%n",
                    count,
                    USER_COUNT,
                    successful.get(),
                    failed.get()
            );
        }
    }

    private static void writeCredentials(
            BlockingQueue<String> credentialQueue) {

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             OUTPUT_FILE,
                             StandardCharsets.UTF_8,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING
                     )) {

            while (true) {

                String credentials =
                        credentialQueue.take();

                if ("__STOP__".equals(credentials)) {
                    break;
                }

                writer.write(credentials);
                writer.newLine();
            }

            writer.flush();

        } catch (IOException |
                 InterruptedException e) {

            throw new RuntimeException(
                    "Credential writer failed",
                    e
            );
        }
    }
}