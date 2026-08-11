package com.aces.tennosquad.dto.listing;

import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.RefinementLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateHostListingRequest(



        Long missionId,

        @NotNull(message = "Mission source is required")
        MissionSource missionSource,

        Long relicId,

        RefinementLevel refinementLevel,

        @Min(value = 1, message = "Player count must be at least 1")
        @Max(value = 4, message = "Player count cannot exceed 4")
        int playerCount,

        @Min(value = 1, message = "Maximum players must be at least 1")
        @Max(value = 4, message = "Maximum players cannot exceed 4")
        int maxPlayers,

        @Positive(message = "Expected duration must be greater than 0")
        Integer expectedDurationMinutes

) {
}