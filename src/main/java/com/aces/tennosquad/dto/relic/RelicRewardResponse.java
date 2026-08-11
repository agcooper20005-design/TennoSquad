package com.aces.tennosquad.dto.relic;

import com.aces.tennosquad.model.enums.RewardRarity;

import java.math.BigDecimal;

public record RelicRewardResponse(
        Long id,
        String itemName,
        RewardRarity rarity,
        BigDecimal chance
) {
}