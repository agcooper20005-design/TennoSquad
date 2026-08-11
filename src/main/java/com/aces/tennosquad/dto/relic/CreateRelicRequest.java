package com.aces.tennosquad.dto.relic;

import com.aces.tennosquad.model.enums.RelicEra;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRelicRequest(

        @NotBlank(message = "Relic name is required")
        @Size(max = 100, message = "Relic name cannot exceed 100 characters")
        String name,

        @NotNull(message = "Relic era is required")
        RelicEra era,

        boolean active

) {
}