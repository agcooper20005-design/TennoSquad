package com.aces.tennosquad.error_logs_and_testing;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TennoSquad V1 workflow/regression runner.
 *
 * IMPORTANT:
 * - Creates test USERS.
 * - NEVER calls any relic write/maintenance endpoint.
 * - NEVER creates, updates, deletes, fetches, or repopulates relics.
 * - Reads existing relics only.
 * - Reads existing missions only.
 * - Uses separate CookieManagers so each simulated user has an independent session.
 *
 * Expected backend: http://localhost:8093
 */
public class TennoSquadWorkflowRunner {

    private static final String BASE_URL = "http://localhost:8093";
    private static final String TEST_PASSWORD = "WorkflowPassword!123";

    private static int passed = 0;
    private static int failed = 0;

    private static final HttpClient ANONYMOUS = newClient();

    private static final List<TestUser> USERS = new ArrayList<>();

    private record TestUser(
            String userName,
            String warframeUserName,
            String password,
            HttpClient client,
            long id
    ) {}

    private record Response(int status, String body) {}

    private record Relic(long id, String name, String era, boolean active) {}

    private record Mission(
            long id,
            String missionType,
            String missionSource,
            String fissureEra,
            boolean active
    ) {}

    public static void main(String[] args) {
        try {
            runWorkflow();
        } catch (Exception e) {
            fail("WORKFLOW ABORTED");
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }

        System.out.println();
        System.out.println("==========================================");
        System.out.println(" TEST SUMMARY");
        System.out.println("==========================================");
        System.out.println("Passed : " + passed + " / " + (passed + failed));
        System.out.println("Failed : " + failed + " / " + (passed + failed));

        if (failed == 0) {
            System.out.println();
            System.out.println("==========================================");
            System.out.println(" TENNOSQUAD WORKFLOW: ALL TESTS PASSED");
            System.out.println("==========================================");
        } else {
            System.out.println();
            System.out.println("--------------- FAILURES -----------------");
        }

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runWorkflow() throws Exception {
        System.out.println("Backend: " + BASE_URL);

        // --------------------------------------------------
        // 1. Backend + clean-database user creation
        // --------------------------------------------------
        expectStatus("Backend reachable", get(ANONYMOUS, "/api/missions/active"), 200);

        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        for (int i = 1; i <= 10; i++) {
            HttpClient client = newClient();
            String userName = "workflow_" + i + "_" + run;
            String warframeName = "WorkflowTenno" + i + "_" + run;

            Response created = postJson(
                    ANONYMOUS,
                    "/api/users",
                    """
                    {
                      "userName": "%s",
                      "warframeUserName": "%s",
                      "password": "%s"
                    }
                    """.formatted(userName, warframeName, TEST_PASSWORD)
            );

            expectAnyStatus("Create user " + i, created, 200, 201);
            long id = requireLong(created.body(), "id", "Created user " + i + " response did not contain an ID");

            USERS.add(new TestUser(
                    userName,
                    warframeName,
                    TEST_PASSWORD,
                    client,
                    id
            ));
        }

        // --------------------------------------------------
        // 2. Authentication + independent sessions
        // --------------------------------------------------
        expectStatus(
                "Reject /me without session",
                get(ANONYMOUS, "/api/auth/me"),
                401
        );

        TestUser first = USERS.get(0);

        expectStatus(
                "Reject incorrect password",
                postJson(
                        first.client(),
                        "/api/auth/login",
                        """
                        {
                          "userName": "%s",
                          "password": "DefinitelyWrongPassword!"
                        }
                        """.formatted(first.userName())
                ),
                401
        );

        for (int i = 0; i < USERS.size(); i++) {
            TestUser user = USERS.get(i);

            expectStatus(
                    "Login user " + (i + 1),
                    login(user),
                    200
            );

            Response me = get(user.client(), "/api/auth/me");
            expectStatus("User " + (i + 1) + " /me", me, 200);
            expectTrue(
                    "User " + (i + 1) + " /me ID matches",
                    readLong(me.body(), "id") == user.id()
            );
            expectTrue(
                    "User " + (i + 1) + " /me role USER",
                    "USER".equals(readString(me.body(), "role"))
            );
        }

        // --------------------------------------------------
        // 3. Admin/user-data boundaries
        // --------------------------------------------------
        expectStatus(
                "Anonymous cannot list users",
                get(ANONYMOUS, "/api/users"),
                401
        );

        expectStatus(
                "Normal user cannot list users",
                get(first.client(), "/api/users"),
                403
        );

        expectStatus(
                "Normal user cannot GET user by ID",
                get(first.client(), "/api/users/" + first.id()),
                403
        );

        expectStatus(
                "Normal user cannot GET user by username",
                get(first.client(), "/api/users/username/" + first.userName()),
                403
        );

        expectStatus(
                "Normal user cannot GET user by Warframe username",
                get(first.client(), "/api/users/warframe-username/" + first.warframeUserName()),
                403
        );

        // --------------------------------------------------
        // 4. Protected mission/relic/maintenance/admin writes
        // --------------------------------------------------
        String fakeMission = """
                {
                  "externalMissionId": "WORKFLOW-PROTECTED-WRITE",
                  "missionType": "DEFENSE",
                  "node": "Workflow Node",
                  "faction": "GRINEER",
                  "missionSource": "FISSURE",
                  "steelPath": false,
                  "fissure": true,
                  "fissureEra": "LITH",
                  "expiry": "2030-01-01T00:00:00Z"
                }
                """;

        expectStatus(
                "Anonymous cannot create/sync mission",
                postJson(ANONYMOUS, "/api/missions", fakeMission),
                401
        );

        expectStatus(
                "Normal user cannot create/sync mission",
                postJson(first.client(), "/api/missions", fakeMission),
                403
        );

        expectStatus(
                "Anonymous cannot sync missions maintenance",
                postNoBody(ANONYMOUS, "/api/maintenance/missions/sync"),
                401
        );

        expectStatus(
                "Normal user cannot sync missions maintenance",
                postNoBody(first.client(), "/api/maintenance/missions/sync"),
                403
        );

        // --------------------------------------------------
        // 5. Read existing relic/mission data.
        //    NO RELIC MUTATION occurs anywhere in this runner.
        // --------------------------------------------------
        Response relicResponse = get(ANONYMOUS, "/api/relics");
        expectStatus("Read existing relics", relicResponse, 200);

        List<Relic> relics = parseRelics(relicResponse.body());
        Relic activeRelic = relics.stream()
                .filter(Relic::active)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No active relic exists. Populate relics before running the workflow."
                ));

        Response missionResponse = get(ANONYMOUS, "/api/missions/active");
        expectStatus("Read active missions", missionResponse, 200);
        List<Mission> missions = parseMissions(missionResponse.body());

        Mission compatibleMission = findCompatibleMission(activeRelic, missions);

        // --------------------------------------------------
        // 6. Anonymous listing creation rejected
        // --------------------------------------------------
        expectStatus(
                "Reject anonymous listing creation",
                postJson(
                        ANONYMOUS,
                        "/api/host-listings",
                        fissureListingJson(
                                compatibleMission == null ? null : compatibleMission.id(),
                                activeRelic.id(),
                                1,
                                4
                        )
                ),
                401
        );

        // --------------------------------------------------
        // 7. Four-player Fissure simulation
        // --------------------------------------------------
        TestUser host = USERS.get(0);
        TestUser attacker = USERS.get(4);

        Response fissureCreated = postJson(
                host.client(),
                "/api/host-listings",
                fissureListingJson(
                        compatibleMission == null ? null : compatibleMission.id(),
                        activeRelic.id(),
                        1,
                        4
                )
        );

        expectAnyStatus("Host creates Fissure listing", fissureCreated, 200, 201);
        long fissureListingId = requireLong(
                fissureCreated.body(),
                "id",
                "Fissure listing did not contain ID"
        );

        expectTrue(
                "Public listing does not expose login username",
                !fissureCreated.body().contains("\"userName\"")
        );
        expectTrue(
                "Public listing does not expose user role",
                !fissureCreated.body().contains("\"role\"")
        );
        expectTrue(
                "Public listing does not expose user createdAt inside host",
                !hostObject(fissureCreated.body()).contains("\"createdAt\"")
        );
        expectTrue(
                "Public listing exposes Warframe username",
                fissureCreated.body().contains(host.warframeUserName())
        );
        expectTrue(
                "Fissure listing starts OPEN",
                "OPEN".equals(readString(fissureCreated.body(), "status"))
        );

        expectStatus(
                "Anonymous cannot change player count",
                postJson(
                        ANONYMOUS,
                        "/api/host-listings/" + fissureListingId + "/player-count",
                        "{\"playerCount\":2}"
                ),
                401
        );

        expectStatus(
                "Other user cannot change host player count",
                postJson(
                        attacker.client(),
                        "/api/host-listings/" + fissureListingId + "/player-count",
                        "{\"playerCount\":2}"
                ),
                403
        );

        // Simulate players 2, 3 and 4 joining via whisper endpoint,
        // while the host owns the authoritative player-count update.
        for (int count = 2; count <= 4; count++) {
            TestUser joiningUser = USERS.get(count - 1);

            Response join = postNoBody(
                    joiningUser.client(),
                    "/api/host-listings/" + fissureListingId + "/join"
            );
            expectStatus("Player " + count + " can request Fissure join", join, 200);
            expectTrue(
                    "Join response contains host Warframe name for player " + count,
                    join.body().contains(host.warframeUserName())
            );

            Response update = postJson(
                    host.client(),
                    "/api/host-listings/" + fissureListingId + "/player-count",
                    "{\"playerCount\":" + count + "}"
            );
            expectStatus("Host sets Fissure count to " + count + " / 4", update, 200);

            if (count < 4) {
                expectTrue(
                        count + " / 4 remains OPEN",
                        "OPEN".equals(readString(update.body(), "status"))
                );
            } else {
                expectTrue(
                        "4 / 4 becomes FULL",
                        "FULL".equals(readString(update.body(), "status"))
                );
            }
        }

        expectStatus(
                "Reject join on FULL Fissure listing",
                postNoBody(USERS.get(5).client(), "/api/host-listings/" + fissureListingId + "/join"),
                400
        );

        // --------------------------------------------------
        // 8. Regression #7: FULL listing still counts duplicate
        // --------------------------------------------------
        if (compatibleMission != null) {
            Response duplicateWhileFull = postJson(
                    host.client(),
                    "/api/host-listings",
                    fissureListingJson(
                            compatibleMission.id(),
                            activeRelic.id(),
                            1,
                            4
                    )
            );

            expectAnyStatus(
                    "Reject duplicate mission listing while original is FULL",
                    duplicateWhileFull,
                    400, 409
            );
        }

        // --------------------------------------------------
        // 9. Ownership + close lifecycle
        // --------------------------------------------------
        expectStatus(
                "Other user cannot close host listing",
                patchNoBody(
                        attacker.client(),
                        "/api/host-listings/" + fissureListingId + "/close"
                ),
                403
        );

        Response closed = patchNoBody(
                host.client(),
                "/api/host-listings/" + fissureListingId + "/close"
        );
        expectStatus("Host closes Fissure listing", closed, 200);
        expectTrue(
                "Fissure listing becomes CLOSED",
                "CLOSED".equals(readString(closed.body(), "status"))
        );

        expectStatus(
                "Reject join on CLOSED listing",
                postNoBody(
                        USERS.get(6).client(),
                        "/api/host-listings/" + fissureListingId + "/join"
                ),
                400
        );

        // Closed listing should no longer block same concrete mission.
        if (compatibleMission != null) {
            Response recreateAfterClose = postJson(
                    host.client(),
                    "/api/host-listings",
                    fissureListingJson(
                            compatibleMission.id(),
                            activeRelic.id(),
                            1,
                            4
                    )
            );
            expectAnyStatus(
                    "Closed listing allows new listing for same mission",
                    recreateAfterClose,
                    200, 201
            );

            long recreatedId = requireLong(
                    recreateAfterClose.body(),
                    "id",
                    "Recreated listing did not contain ID"
            );

            expectStatus(
                    "Cleanup recreated Fissure listing",
                    patchNoBody(host.client(), "/api/host-listings/" + recreatedId + "/close"),
                    200
            );
        }

        // --------------------------------------------------
        // 10. Arbitration simulation
        // --------------------------------------------------
        TestUser arbitrationHost = USERS.get(4);

        Response arbitration = postJson(
                arbitrationHost.client(),
                "/api/host-listings",
                """
                {
                  "missionSource": "ARBITRATION",
                  "playerCount": 1,
                  "maxPlayers": 4,
                  "expectedDurationMinutes": 30
                }
                """
        );

        expectAnyStatus("Create Arbitration listing", arbitration, 200, 201);
        long arbitrationId = requireLong(
                arbitration.body(),
                "id",
                "Arbitration listing did not contain ID"
        );

        expectTrue(
                "Arbitration has no mission",
                isJsonNull(arbitration.body(), "mission")
        );
        expectTrue(
                "Arbitration has no relic",
                isJsonNull(arbitration.body(), "relic")
        );

        for (int count = 2; count <= 4; count++) {
            TestUser joiner = USERS.get(count + 3);

            Response join = postNoBody(
                    joiner.client(),
                    "/api/host-listings/" + arbitrationId + "/join"
            );
            expectStatus("Arbitration player " + count + " can request join", join, 200);
            expectTrue(
                    "Arbitration whisper generated for player " + count,
                    join.body().toLowerCase().contains("arbitration")
            );

            Response update = postJson(
                    arbitrationHost.client(),
                    "/api/host-listings/" + arbitrationId + "/player-count",
                    "{\"playerCount\":" + count + "}"
            );
            expectStatus("Arbitration host sets count " + count + " / 4", update, 200);
        }

        Response arbitrationClosed = patchNoBody(
                arbitrationHost.client(),
                "/api/host-listings/" + arbitrationId + "/close"
        );
        expectStatus("Close Arbitration listing", arbitrationClosed, 200);

        // --------------------------------------------------
        // 11. Relic-only listing + mission assignment regression #2
        // --------------------------------------------------
        TestUser relicHost = USERS.get(8);

        Response relicOnly = postJson(
                relicHost.client(),
                "/api/host-listings",
                fissureListingJson(null, activeRelic.id(), 1, 4)
        );

        expectAnyStatus("Create relic-only Fissure listing", relicOnly, 200, 201);
        long relicOnlyId = requireLong(
                relicOnly.body(),
                "id",
                "Relic-only listing did not contain ID"
        );

        expectTrue(
                "Relic-only listing starts without mission",
                isJsonNull(relicOnly.body(), "mission")
        );

        if (compatibleMission != null) {
            expectStatus(
                    "Anonymous cannot assign mission",
                    patchJson(
                            ANONYMOUS,
                            "/api/host-listings/" + relicOnlyId + "/mission",
                            "{\"missionId\":" + compatibleMission.id() + "}"
                    ),
                    401
            );

            expectStatus(
                    "Other user cannot assign mission",
                    patchJson(
                            USERS.get(9).client(),
                            "/api/host-listings/" + relicOnlyId + "/mission",
                            "{\"missionId\":" + compatibleMission.id() + "}"
                    ),
                    403
            );

            Response assigned = patchJson(
                    relicHost.client(),
                    "/api/host-listings/" + relicOnlyId + "/mission",
                    "{\"missionId\":" + compatibleMission.id() + "}"
            );
            expectStatus("Owner assigns compatible mission", assigned, 200);
            expectTrue(
                    "Assigned mission ID matches",
                    readNestedLong(assigned.body(), "mission", "id") == compatibleMission.id()
            );

            Mission incompatible = findIncompatibleMission(activeRelic, missions);
            if (incompatible != null) {
                Response rejected = patchJson(
                        relicHost.client(),
                        "/api/host-listings/" + relicOnlyId + "/mission",
                        "{\"missionId\":" + incompatible.id() + "}"
                );

                expectStatus(
                        "Reject changing relic listing to incompatible mission",
                        rejected,
                        400
                );
            } else {
                System.out.println("[INFO] No incompatible active Fissure mission exists; incompatibility regression not executed.");
            }
        } else {
            System.out.println("[INFO] No compatible active mission exists; mission-assignment regression not executed.");
        }

        expectStatus(
                "Close relic-only workflow listing",
                patchNoBody(relicHost.client(), "/api/host-listings/" + relicOnlyId + "/close"),
                200
        );

        // --------------------------------------------------
        // 12. Validation attacks
        // --------------------------------------------------
        TestUser validationUser = USERS.get(9);

        expectStatus(
                "Reject empty Fissure listing",
                postJson(
                        validationUser.client(),
                        "/api/host-listings",
                        """
                        {
                          "missionSource": "FISSURE",
                          "playerCount": 1,
                          "maxPlayers": 4
                        }
                        """
                ),
                400
        );

        expectStatus(
                "Reject relic without refinement",
                postJson(
                        validationUser.client(),
                        "/api/host-listings",
                        """
                        {
                          "missionSource": "FISSURE",
                          "relicId": %d,
                          "playerCount": 1,
                          "maxPlayers": 4
                        }
                        """.formatted(activeRelic.id())
                ),
                400
        );

        expectStatus(
                "Reject refinement without relic",
                postJson(
                        validationUser.client(),
                        "/api/host-listings",
                        """
                        {
                          "missionSource": "FISSURE",
                          "refinementLevel": "RADIANT",
                          "playerCount": 1,
                          "maxPlayers": 4
                        }
                        """
                ),
                400
        );

        expectStatus(
                "Reject Arbitration with relic",
                postJson(
                        validationUser.client(),
                        "/api/host-listings",
                        """
                        {
                          "missionSource": "ARBITRATION",
                          "relicId": %d,
                          "refinementLevel": "RADIANT",
                          "playerCount": 1,
                          "maxPlayers": 4
                        }
                        """.formatted(activeRelic.id())
                ),
                400
        );

        expectStatus(
                "Reject player count zero",
                postJson(
                        validationUser.client(),
                        "/api/host-listings",
                        """
                        {
                          "missionSource": "ARBITRATION",
                          "playerCount": 0,
                          "maxPlayers": 4
                        }
                        """
                ),
                400
        );

        expectStatus(
                "Reject max players above four",
                postJson(
                        validationUser.client(),
                        "/api/host-listings",
                        """
                        {
                          "missionSource": "ARBITRATION",
                          "playerCount": 1,
                          "maxPlayers": 5
                        }
                        """
                ),
                400
        );

        // --------------------------------------------------
        // 13. Logout/session invalidation
        // --------------------------------------------------
        TestUser logoutUser = USERS.get(0);

        expectStatus(
                "Logout user",
                postNoBody(logoutUser.client(), "/api/auth/logout"),
                200
        );

        expectStatus(
                "/me rejected after logout",
                get(logoutUser.client(), "/api/auth/me"),
                401
        );

        expectStatus(
                "Logged-out user cannot create listing",
                postJson(
                        logoutUser.client(),
                        "/api/host-listings",
                        """
                        {
                          "missionSource": "ARBITRATION",
                          "playerCount": 1,
                          "maxPlayers": 4
                        }
                        """
                ),
                401
        );

        expectStatus(
                "Re-login after logout",
                login(logoutUser),
                200
        );

        expectStatus(
                "/me restored after re-login",
                get(logoutUser.client(), "/api/auth/me"),
                200
        );

        // --------------------------------------------------
        // 14. Final reads
        // --------------------------------------------------
        expectStatus("Final relics read", get(ANONYMOUS, "/api/relics"), 200);
        expectStatus("Final missions read", get(ANONYMOUS, "/api/missions"), 200);
        expectStatus("Final listings read", get(ANONYMOUS, "/api/host-listings"), 200);
        expectStatus("Final open listings read", get(ANONYMOUS, "/api/host-listings/open"), 200);
    }

    // ======================================================
    // Payload helpers
    // ======================================================

    private static String fissureListingJson(
            Long missionId,
            long relicId,
            int playerCount,
            int maxPlayers
    ) {
        String missionLine = missionId == null
                ? ""
                : "\"missionId\":" + missionId + ",";

        return """
                {
                  %s
                  "missionSource": "FISSURE",
                  "relicId": %d,
                  "refinementLevel": "RADIANT",
                  "playerCount": %d,
                  "maxPlayers": %d,
                  "expectedDurationMinutes": 20
                }
                """.formatted(
                missionLine,
                relicId,
                playerCount,
                maxPlayers
        );
    }

    private static Response login(TestUser user) throws Exception {
        return postJson(
                user.client(),
                "/api/auth/login",
                """
                {
                  "userName": "%s",
                  "password": "%s"
                }
                """.formatted(user.userName(), user.password())
        );
    }

    // ======================================================
    // Existing-data parsing
    // ======================================================

    private static List<Relic> parseRelics(String json) {
        List<Relic> result = new ArrayList<>();

        for (String object : splitTopLevelObjects(json)) {
            Long id = readLongNullable(object, "id");
            String name = readString(object, "name");
            String era = readString(object, "era");
            Boolean active = readBooleanNullable(object, "active");

            if (id != null && name != null && era != null && active != null) {
                result.add(new Relic(id, name, era, active));
            }
        }

        return result;
    }

    private static List<Mission> parseMissions(String json) {
        List<Mission> result = new ArrayList<>();

        for (String object : splitTopLevelObjects(json)) {
            Long id = readLongNullable(object, "id");
            String type = readString(object, "missionType");
            String source = readString(object, "missionSource");
            String era = readString(object, "fissureEra");
            Boolean active = readBooleanNullable(object, "active");

            if (id != null && source != null && active != null) {
                result.add(new Mission(id, type, source, era, active));
            }
        }

        return result;
    }

    private static Mission findCompatibleMission(Relic relic, List<Mission> missions) {
        return missions.stream()
                .filter(Mission::active)
                .filter(m -> "FISSURE".equals(m.missionSource()))
                .filter(m -> erasCompatible(relic.era(), m.fissureEra()))
                .findFirst()
                .orElse(null);
    }

    private static Mission findIncompatibleMission(Relic relic, List<Mission> missions) {
        return missions.stream()
                .filter(Mission::active)
                .filter(m -> "FISSURE".equals(m.missionSource()))
                .filter(m -> m.fissureEra() != null)
                .filter(m -> !erasCompatible(relic.era(), m.fissureEra()))
                .findFirst()
                .orElse(null);
    }

    private static boolean erasCompatible(String relicEra, String fissureEra) {
        if (relicEra == null || fissureEra == null) {
            return false;
        }

        if ("OMNIA".equals(fissureEra)) {
            return List.of("LITH", "MESO", "NEO", "AXI").contains(relicEra);
        }

        return relicEra.equals(fissureEra);
    }

    // ======================================================
    // HTTP
    // ======================================================

    private static HttpClient newClient() {
        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        return HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private static Response get(HttpClient client, String path) throws Exception {
        return send(
                client,
                HttpRequest.newBuilder(URI.create(BASE_URL + path))
                        .GET()
                        .build()
        );
    }

    private static Response postNoBody(HttpClient client, String path) throws Exception {
        return send(
                client,
                HttpRequest.newBuilder(URI.create(BASE_URL + path))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build()
        );
    }

    private static Response patchNoBody(HttpClient client, String path) throws Exception {
        return send(
                client,
                HttpRequest.newBuilder(URI.create(BASE_URL + path))
                        .method("PATCH", HttpRequest.BodyPublishers.noBody())
                        .build()
        );
    }

    private static Response postJson(HttpClient client, String path, String json) throws Exception {
        return send(
                client,
                HttpRequest.newBuilder(URI.create(BASE_URL + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build()
        );
    }

    private static Response patchJson(HttpClient client, String path, String json) throws Exception {
        return send(
                client,
                HttpRequest.newBuilder(URI.create(BASE_URL + path))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                        .build()
        );
    }

    private static Response send(HttpClient client, HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return new Response(response.statusCode(), response.body());
    }

    // ======================================================
    // Assertions
    // ======================================================

    private static void expectStatus(String name, Response response, int expected) {
        if (response.status() == expected) {
            pass(name);
            return;
        }

        fail(name);
        System.err.println(
                "Expected HTTP " + expected
                        + " but received " + response.status()
        );
        if (!response.body().isBlank()) {
            System.err.println(response.body());
        }
    }

    private static void expectAnyStatus(
            String name,
            Response response,
            int... expectedStatuses
    ) {
        for (int expected : expectedStatuses) {
            if (response.status() == expected) {
                pass(name);
                return;
            }
        }

        fail(name);
        System.err.println(
                "Expected HTTP " + java.util.Arrays.toString(expectedStatuses)
                        + " but received " + response.status()
        );
        if (!response.body().isBlank()) {
            System.err.println(response.body());
        }
    }

    private static void expectTrue(String name, boolean condition) {
        if (condition) {
            pass(name);
        } else {
            fail(name);
        }
    }

    private static void pass(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void fail(String name) {
        failed++;
        System.err.println("[FAIL] " + name);
    }

    // ======================================================
    // Minimal JSON helpers
    // ======================================================

    private static long requireLong(
            String json,
            String field,
            String message
    ) {
        Long value = readLongNullable(json, field);
        if (value == null) {
            throw new IllegalStateException(message + "\n" + json);
        }
        return value;
    }

    private static long readLong(String json, String field) {
        Long value = readLongNullable(json, field);
        return value == null ? Long.MIN_VALUE : value;
    }

    private static Long readLongNullable(String json, String field) {
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+)"
        ).matcher(json);

        if (!matcher.find()) {
            return null;
        }

        return Long.parseLong(matcher.group(1));
    }

    private static String readString(String json, String field) {
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\""
        ).matcher(json);

        return matcher.find() ? matcher.group(1) : null;
    }

    private static Boolean readBooleanNullable(String json, String field) {
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*(true|false)"
        ).matcher(json);

        return matcher.find()
                ? Boolean.parseBoolean(matcher.group(1))
                : null;
    }

    private static boolean isJsonNull(String json, String field) {
        return Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*null"
        ).matcher(json).find();
    }

    private static long readNestedLong(
            String json,
            String objectField,
            String childField
    ) {
        String object = extractObject(json, objectField);
        return readLong(object, childField);
    }

    private static String hostObject(String json) {
        return extractObject(json, "host");
    }

    private static String extractObject(String json, String field) {
        int fieldIndex = json.indexOf("\"" + field + "\"");
        if (fieldIndex < 0) {
            return "";
        }

        int start = json.indexOf('{', fieldIndex);
        if (start < 0) {
            return "";
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(start, i + 1);
                    }
                }
            }
        }

        return "";
    }

    private static List<String> splitTopLevelObjects(String json) {
        List<String> objects = new ArrayList<>();

        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;

                if (depth == 0 && start >= 0) {
                    objects.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return objects;
    }
}
