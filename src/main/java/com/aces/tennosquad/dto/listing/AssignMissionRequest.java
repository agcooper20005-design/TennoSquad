package com.aces.tennosquad.dto.listing;

import jakarta.validation.constraints.NotNull;

public record AssignMissionRequest(

        @NotNull(message = "Mission ID is required")
        Long missionId

) {
}