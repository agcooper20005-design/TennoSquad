package com.aces.tennosquad.dto.relic;

import com.aces.tennosquad.model.enums.RelicEra;

import java.util.List;

public record RelicResponse(
        Long id,
        String name,
        RelicEra era,
        boolean active,
        List<RelicRewardResponse> rewards
) {
}