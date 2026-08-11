package com.aces.tennosquad.integration;

import com.aces.tennosquad.dto.mission.MissionResponse;
import com.aces.tennosquad.dto.mission.SyncMissionRequest;
import com.aces.tennosquad.exception.WarframeApiException;
import com.aces.tennosquad.integration.dto.WarframeArbitrationResponse;
import com.aces.tennosquad.integration.dto.WarframeFissureResponse;
import com.aces.tennosquad.model.enums.Faction;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.MissionType;
import com.aces.tennosquad.model.enums.RelicEra;
import com.aces.tennosquad.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WarframeSyncService {

    private static final String FISSURES_ENDPOINT = "/pc/fissures";
    private static final String ARBITRATION_ENDPOINT = "/pc/arbitration";

    private final RestClient warframeRestClient;
    private final MissionService missionService;

    /**
     * Synchronizes all currently supported mission sources.
     */
    public SyncResult syncCurrentMissions() {

        List<MissionResponse> synchronizedMissions = new ArrayList<>();

        synchronizedMissions.addAll(syncFissures());


        /// SYNC ARBITRATION DEACTIVATED
//        syncArbitration().ifPresent(
//                synchronizedMissions::add
//        );

        int deactivatedCount =
                missionService.deactivateExpiredMissions();

        int deletedCount =
                missionService.deleteInactiveMissions();

        return new SyncResult(
                synchronizedMissions.size(),
                deactivatedCount,
                deletedCount
        );
    }

    /**
     * Synchronizes all active Void Fissures, including Steel Path fissures.
     */
    public List<MissionResponse> syncFissures() {

        List<WarframeFissureResponse> fissures =
                fetchFissures();

        return fissures.stream()
                .map(this::mapFissureToSyncRequest)
                .map(missionService::syncMission)
                .toList();
    }

    /**
     * Synchronizes the currently active Arbitration.
     *
     * The endpoint may return no active Arbitration, so this method returns
     * Optional rather than throwing ResourceNotFoundException.
     */
    private Optional<MissionResponse> syncArbitration() {

        WarframeArbitrationResponse response = fetchArbitration();

        if (response == null) {

            return Optional.empty();
        }

        MissionResponse synchronizedMission =
                missionService.syncMission(
                        mapArbitrationToSyncRequest(response)
                );

        return Optional.of(synchronizedMission);
    }


    private List<WarframeFissureResponse> fetchFissures() {
        try {
            List<WarframeFissureResponse> fissures =
                    warframeRestClient.get()
                            .uri(FISSURES_ENDPOINT)
                            .retrieve()
                            .body(
                                    new ParameterizedTypeReference<>() {
                                    }
                            );

            if (fissures == null) {
                return List.of();
            }

            return fissures;

        } catch (RestClientException exception) {
            throw new WarframeApiException(
                    "Failed to retrieve current Void Fissures",
                    exception
            );
        }
    }

    private WarframeArbitrationResponse fetchArbitration() {
        try {
            return warframeRestClient.get()
                    .uri(ARBITRATION_ENDPOINT)
                    .retrieve()
                    .body(WarframeArbitrationResponse.class);

        } catch (RestClientException exception) {
            throw new WarframeApiException(
                    "Failed to retrieve the current Arbitration",
                    exception
            );
        }
    }

    private SyncMissionRequest mapFissureToSyncRequest(
            WarframeFissureResponse fissure
    ) {
        validateFissureResponse(fissure);

        return new SyncMissionRequest(
                buildExternalId("FISSURE", fissure.id()),
                mapMissionType(fissure.missionType()),
                fissure.node().trim(),
                mapFaction(fissure.enemy()),
                MissionSource.FISSURE,
                fissure.isHard(),
                true,
                mapRelicEra(fissure.tier()),
                fissure.expiry()
        );
    }

    private SyncMissionRequest mapArbitrationToSyncRequest(
            WarframeArbitrationResponse response
    ) {
        if (response.type() == null
                || response.type().isBlank()
                || response.type().equalsIgnoreCase("Unknown")) {

            throw new WarframeApiException(
                    "Warframe Arbitration API returned no usable mission type"
            );
        }

        return new SyncMissionRequest(
                buildExternalId("ARBITRATION", response.id()),
                mapMissionType(response.type()),
                response.node(),
                mapFaction(response.enemy()),
                MissionSource.ARBITRATION,
                false,
                false,
                null,
                response.expiry()
        );
    }

    private String buildExternalId(
            String source,
            String externalId
    ) {
        return source + ":" + externalId;
    }

    private MissionType mapMissionType(String apiMissionType) {

        if (apiMissionType == null || apiMissionType.isBlank()) {
            throw new WarframeApiException(
                    "Warframe API mission type was missing"
            );
        }

        String enumName = normalizeEnumName(apiMissionType);

        try {
            return MissionType.valueOf(enumName);

        } catch (IllegalArgumentException exception) {
            throw new WarframeApiException(
                    "Unsupported Warframe mission type: "
                            + apiMissionType
            );
        }
    }

    private RelicEra mapRelicEra(String tier) {
        if (tier == null || tier.isBlank()) {
            throw new WarframeApiException(
                    "Warframe API fissure tier was missing"
            );
        }

        try {
            return RelicEra.valueOf(
                    tier.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new WarframeApiException(
                    "Unsupported fissure tier: " + tier,
                    exception
            );
        }
    }

    private Faction mapFaction(String apiFaction) {

        if (apiFaction == null || apiFaction.isBlank()) {
            throw new WarframeApiException(
                    "Warframe API faction was missing"
            );
        }

        String enumName = normalizeEnumName(apiFaction);

        try {
            return Faction.valueOf(enumName);

        } catch (IllegalArgumentException exception) {
            throw new WarframeApiException(
                    "Unsupported Warframe faction: "
                            + apiFaction
            );
        }
    }

    private String normalizeEnumName(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace("&", "AND")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }


    private void validateFissureResponse(
            WarframeFissureResponse fissure
    ) {
        if (fissure.id() == null || fissure.id().isBlank()) {
            throw new WarframeApiException(
                    "Warframe API returned a fissure without an ID"
            );
        }

        if (fissure.node() == null || fissure.node().isBlank()) {
            throw new WarframeApiException(
                    "Warframe API returned a fissure without a node"
            );
        }

        if (fissure.expiry() == null) {
            throw new WarframeApiException(
                    "Warframe API returned a fissure without an expiry"
            );
        }
    }

    private void validateArbitrationResponse(
            WarframeArbitrationResponse arbitration
    ) {
        if (arbitration.id() == null
                || arbitration.id().isBlank()) {

            throw new WarframeApiException(
                    "Warframe API returned an Arbitration without an ID"
            );
        }

        if (arbitration.node() == null
                || arbitration.node().isBlank()) {

            throw new WarframeApiException(
                    "Warframe API returned an Arbitration without a node"
            );
        }

        if (arbitration.expiry() == null) {
            throw new WarframeApiException(
                    "Warframe API returned an Arbitration without an expiry"
            );
        }
    }

    public record SyncResult(
            int synchronizedMissions,
            int deactivatedMissions,
            int deletedMissions
    ) {
    }
}