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

public class UserDatabaseSeeder {

    private static final String API_URL =
            "http://localhost:8093/api/users";

    private static final int USER_COUNT = 50_000;

    private static final Path OUTPUT_FILE =
            Path.of("created-users.txt");

    public static void main(String[] args) {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        int successful = 0;
        int failed = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                OUTPUT_FILE,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

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

                try {
                    HttpResponse<String> response =
                            client.send(
                                    request,
                                    HttpResponse.BodyHandlers.ofString()
                            );

                    int statusCode = response.statusCode();

                    if (statusCode >= 200 && statusCode < 300) {

                        successful++;

                        // Log only users that were successfully created
                        writer.write(
                                userName + ":" + password
                        );
                        writer.newLine();

                    } else {

                        failed++;

                        System.err.printf(
                                "FAILED [%d/%d] %s -> HTTP %d: %s%n",
                                i,
                                USER_COUNT,
                                userName,
                                statusCode,
                                response.body()
                        );
                    }

                } catch (IOException e) {

                    failed++;

                    System.err.printf(
                            "REQUEST ERROR [%d/%d] %s: %s%n",
                            i,
                            USER_COUNT,
                            userName,
                            e.getMessage()
                    );

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    System.err.println("Seeder interrupted.");
                    break;
                }

                if (i % 100 == 0) {
                    writer.flush();

                    System.out.printf(
                            "Progress: %,d / %,d | Successful: %,d | Failed: %,d%n",
                            i,
                            USER_COUNT,
                            successful,
                            failed
                    );
                }
            }

            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not write credentials file.",
                    e
            );
        }

        System.out.println();
        System.out.println("Finished.");
        System.out.printf("Successful: %,d%n", successful);
        System.out.printf("Failed:     %,d%n", failed);
        System.out.println(
                "Credentials file: "
                        + OUTPUT_FILE.toAbsolutePath()
        );
    }
}