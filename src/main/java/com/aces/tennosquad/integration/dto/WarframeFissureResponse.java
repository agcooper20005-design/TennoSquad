package com.aces.tennosquad.integration.dto;

import com.aces.tennosquad.model.enums.RelicEra;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarframeFissureResponse(
        String id,
        Instant activation,
        Instant expiry,
        String node,
        String missionType,
        String enemy,
        String tier,
        boolean isStorm,
        boolean isHard
) {
}