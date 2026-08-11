package com.aces.tennosquad.service.admin;


import com.aces.tennosquad.dto.listing.HostListingResponse;
import com.aces.tennosquad.dto.mission.MissionResponse;
import com.aces.tennosquad.dto.relic.RelicResponse;
import com.aces.tennosquad.dto.relic.RelicRewardResponse;
import com.aces.tennosquad.dto.user.PublicUserResponse;
import com.aces.tennosquad.dto.user.UserResponse;
import com.aces.tennosquad.exception.ResourceNotFoundException;
import com.aces.tennosquad.model.HostListing;
import com.aces.tennosquad.model.Mission;
import com.aces.tennosquad.model.Relic;
import com.aces.tennosquad.model.User;
import com.aces.tennosquad.model.enums.HostListingStatus;
import com.aces.tennosquad.repository.HostListingRepository;
import com.aces.tennosquad.repository.MissionRepository;
import com.aces.tennosquad.repository.RelicRepository;
import com.aces.tennosquad.repository.UserRepository;
import com.aces.tennosquad.validation.HostListingValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final HostListingRepository hostListingRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final RelicRepository relicRepository;
    private final HostListingValidator hostListingValidator;



    @Transactional
    public HostListingResponse forceCloseListing(Long listingId){
        HostListing listing = findListingById(listingId);

        hostListingValidator.validateListingIsNotClosed(listing);

        listing.setStatus(HostListingStatus.CLOSED);

        return mapToResponse(listing);
    }








    /** PRIVATE HELPERS */

    private HostListing findListingById(Long listingId) {
        return hostListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Host listing not found with ID: " + listingId
                ));
    }


    /** PRIVATE MAPPERS */
    private HostListingResponse mapToResponse(HostListing listing) {
        return new HostListingResponse(
                listing.getId(),
                mapUserToResponse(listing.getHost()),
                mapMissionToResponse(listing.getMission()),
                listing.getMissionSource(),
                mapRelicToResponse(listing.getRelic()),
                listing.getRefinementLevel(),
                listing.getPlayerCount(),
                listing.getMaxPlayers(),
                listing.getStatus(),
                listing.getExpectedDurationMinutes(),
                listing.getCreatedAt()
        );
    }

    private PublicUserResponse mapUserToResponse(User user) {
        return new PublicUserResponse(
                user.getId(),
                user.getWarframeUserName()
        );
    }

    private MissionResponse mapMissionToResponse(Mission mission) {
        if (mission == null) {
            return null;
        }

        long remainingSeconds = mission.getExpiry() == null
                ? 0
                : Math.max(
                0,
                Duration.between(
                        Instant.now(),
                        mission.getExpiry()
                ).getSeconds()
        );

        return new MissionResponse(
                mission.getId(),
                mission.getExternalMissionId(),
                mission.getMissionType(),
                mission.getNode(),
                mission.getFaction(),
                mission.getMissionSource(),
                mission.isSteelPath(),
                mission.isFissure(),
                mission.getFissureEra(),
                mission.isActive(),
                mission.getExpiry(),
                remainingSeconds
        );
    }

    private RelicResponse mapRelicToResponse(Relic relic) {
        if (relic == null) {
            return null;
        }

        List<RelicRewardResponse> rewardResponses =
                relic.getRewards()
                        .stream()
                        .map(reward -> new RelicRewardResponse(
                                reward.getId(),
                                reward.getItemName(),
                                reward.getRarity(),
                                reward.getChance()
                        ))
                        .toList();

        return new RelicResponse(
                relic.getId(),
                relic.getName(),
                relic.getRelicEra(),
                relic.isActive(),
                rewardResponses
        );
    }
}
