package com.aces.tennosquad.repository;

import com.aces.tennosquad.model.Mission;
import com.aces.tennosquad.model.enums.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByMissionType(MissionType missionType);

    List<Mission> findBySteelPath(boolean steelPath);

    List<Mission> findByFissure(boolean fissure);

    Optional<Mission> findByExternalMissionId(String externalMissionId);

    boolean existsByExternalMissionId(String externalMissionId);

    List<Mission> findByExpiryAfterOrExpiryIsNull(Instant currentTime);

    int deleteByExpiryBefore(Instant currentTime);


    List<Mission> findByActiveTrue();

    List<Mission> findByActiveTrueAndMissionType(MissionType missionType);

    List<Mission> findByActiveFalse();

    List<Mission> findByActiveTrueAndExpiryBefore(Instant currentTime);

    int deleteByActiveFalse();

    @Query("""
        SELECT mission
        FROM Mission mission
        WHERE mission.active = false
        AND NOT EXISTS (
            SELECT listing.id
            FROM HostListing listing
            WHERE listing.mission = mission
        )
        """)
    List<Mission> findInactiveMissionsWithoutListings();

}