package com.aces.tennosquad.dto.mission;

import com.aces.tennosquad.model.enums.Faction;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.MissionType;
import com.aces.tennosquad.model.enums.RelicEra;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.time.LocalDateTime;

public record MissionResponse(
        Long id,
        String externalMissionId,
        MissionType missionType,
        String node,
        Faction faction,
        MissionSource missionSource,
        boolean steelPath,
        boolean fissure,
        RelicEra fissureEra,
        boolean active,
        Instant expiry,
        long remainingSeconds
) {
}