package com.aces.tennosquad.service;

import com.aces.tennosquad.dto.mission.MissionResponse;
import com.aces.tennosquad.dto.mission.SyncMissionRequest;
import com.aces.tennosquad.exception.ResourceNotFoundException;
import com.aces.tennosquad.exception.WarframeApiException;
import com.aces.tennosquad.model.Mission;
import com.aces.tennosquad.model.enums.MissionType;
import com.aces.tennosquad.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;

    /** POST */
    @Transactional
    public MissionResponse syncMission(SyncMissionRequest request) {

        Mission mission = missionRepository
                .findByExternalMissionId(request.externalMissionId())
                .orElseGet(Mission::new);

        mission.setExternalMissionId(request.externalMissionId());
        mission.setMissionType(request.missionType());
        mission.setNode(request.node());
        mission.setFaction(request.faction());
        mission.setMissionSource(request.missionSource());
        mission.setSteelPath(request.steelPath());
        mission.setFissureEra(request.fissureEra());
        mission.setFissure(request.fissure());
        mission.setExpiry(request.expiry());

        mission.setActive(determineActiveStatus(request.expiry()));

        Mission savedMission = missionRepository.save(mission);

        return mapToResponse(savedMission);
    }

    /** GET */
    @Transactional(readOnly = true)
    public MissionResponse getMissionById(Long missionId) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mission not found with ID: " + missionId
                ));

        return mapToResponse(mission);
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getAllMissions() {
        return missionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getActiveMissions() {
        return missionRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> getActiveMissionsByType(
            MissionType missionType
    ) {
        return missionRepository
                .findByActiveTrueAndMissionType(missionType)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Finds missions whose expiry time has passed and marks them inactive.
     *
     * @return number of missions marked inactive
     */
    /** POST/PATCH */
    @Transactional
    public int deactivateExpiredMissions() {


        List<Mission> expiredMissions =
                missionRepository.findByActiveTrueAndExpiryBefore(Instant.now());

        expiredMissions.forEach(mission -> mission.setActive(false));

        missionRepository.saveAll(expiredMissions);

        return expiredMissions.size();
    }

    /**
     * Deletes missions previously marked inactive.
     *
     * Warning: deletion may fail if a HostListing still references a mission.
     */
    /** DELETE */
    @Transactional
    public int deleteInactiveMissions() {

        List<Mission> deletableMissions =
                missionRepository.findInactiveMissionsWithoutListings();

        missionRepository.deleteAll(deletableMissions);

        return deletableMissions.size();
    }


    private MissionType mapMissionType(String value) {
        String normalized = normalizeEnumName(value);

        return switch (normalized) {
            case "FREE_ROAM", "BOUNTY" ->
                    MissionType.FREE_ROAM_BOUNTY;

            case "THE_INDEX", "INDEX", "RATHUUM" ->
                    MissionType.ARENA;

            default -> {
                try {
                    yield MissionType.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    throw new WarframeApiException(
                            "Unsupported Warframe mission type: " + value,
                            exception
                    );
                }
            }
        };
    }
    private String normalizeEnumName(String value) {
        if (value == null || value.isBlank()) {
            throw new WarframeApiException(
                    "Warframe API returned a blank enum value"
            );
        }

        return value
                .trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_")
                .replace("/", "_");
    }


    private boolean determineActiveStatus(Instant expiry) {
        return expiry == null || expiry.isAfter(Instant.now());
    }


    private MissionResponse mapToResponse(Mission mission) {
        long remainingSeconds = Math.max(
                0,
                Duration.between(
                        Instant.now(),
                        mission.getExpiry()
                ).getSeconds()
        );

        return new MissionResponse(
                mission.getId(),
                mission.getExternalMissionId(),
                mission.getMissionType(),
                mission.getNode(),
                mission.getFaction(),
                mission.getMissionSource(),
                mission.isSteelPath(),
                mission.isFissure(),
                mission.getFissureEra(),
                mission.isActive(),
                mission.getExpiry(),
                remainingSeconds
        );
    }
}