package com.aces.tennosquad.dto.listing;

import com.aces.tennosquad.dto.mission.MissionResponse;
import com.aces.tennosquad.dto.relic.RelicResponse;
import com.aces.tennosquad.dto.user.PublicUserResponse;
import com.aces.tennosquad.model.enums.HostListingStatus;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.RefinementLevel;

import java.time.LocalDateTime;


/// Response for Host Listing
/// Contains UserResponse & Mission Response & Relic Response
public record HostListingResponse(

        Long id,

        PublicUserResponse host,

        MissionResponse mission,

        MissionSource missionSource,

        RelicResponse relic,

        RefinementLevel refinementLevel,

        int playerCount,

        int maxPlayers,

        HostListingStatus status,

        Integer expectedDurationMinutes,

        LocalDateTime createdAt

) {
}