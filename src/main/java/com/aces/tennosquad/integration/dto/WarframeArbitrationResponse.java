 package com.aces.tennosquad.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarframeArbitrationResponse(
        String id,
        Instant activation,
        Instant expiry,
        String node,
        String type,
        String enemy
) {
}