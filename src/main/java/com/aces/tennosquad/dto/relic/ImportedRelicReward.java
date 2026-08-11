package com.aces.tennosquad.dto.relic;

import com.aces.tennosquad.model.enums.RewardRarity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ImportedRelicReward(

        @NotBlank(message = "Reward item name is required")
        String itemName,

        @NotNull(message = "Reward rarity is required")
        RewardRarity rarity,

        @NotNull(message = "Reward chance is required")
        @DecimalMin(
                value = "0.0",
                inclusive = true,
                message = "Reward chance cannot be less than 0"
        )
        @DecimalMax(
                value = "100.0",
                inclusive = true,
                message = "Reward chance cannot exceed 100"
        )
        BigDecimal chance

) {
}