package com.aces.tennosquad.dto.mission;

import com.aces.tennosquad.model.enums.Faction;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.MissionType;
import com.aces.tennosquad.model.enums.RelicEra;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;

public record SyncMissionRequest(

        @NotBlank(message = "External mission ID is required")
        String externalMissionId,

        @NotNull(message = "Mission type is required")
        MissionType missionType,

        @NotBlank(message = "Mission node is required")
        @Size(max = 100, message = "Mission node cannot exceed 100 characters")
        String node,

        @NotNull(message = "Faction is required")
        Faction faction,

        @NotNull
        MissionSource missionSource,

        boolean steelPath,

        boolean fissure,
        @Enumerated(EnumType.STRING)
        RelicEra fissureEra,

        Instant expiry

) {
}