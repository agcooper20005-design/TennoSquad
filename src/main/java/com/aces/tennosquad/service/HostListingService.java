package com.aces.tennosquad.service;

import com.aces.tennosquad.dto.listing.*;
import com.aces.tennosquad.dto.mission.MissionResponse;
import com.aces.tennosquad.dto.relic.RelicResponse;
import com.aces.tennosquad.dto.relic.RelicRewardResponse;
import com.aces.tennosquad.dto.user.PublicUserResponse;
import com.aces.tennosquad.exception.*;
import com.aces.tennosquad.model.HostListing;
import com.aces.tennosquad.model.Mission;
import com.aces.tennosquad.model.Relic;
import com.aces.tennosquad.model.User;
import com.aces.tennosquad.model.enums.HostListingStatus;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.RelicEra;
import com.aces.tennosquad.repository.HostListingRepository;
import com.aces.tennosquad.repository.MissionRepository;
import com.aces.tennosquad.repository.RelicRepository;
import com.aces.tennosquad.repository.UserRepository;

import com.aces.tennosquad.validation.HostListingValidator;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HostListingService {

    private final HostListingRepository hostListingRepository;
    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final RelicRepository relicRepository;
    private final HostListingValidator hostListingValidator;


    /** POST */
    @Transactional
    public HostListingResponse createListing(CreateHostListingRequest request) {
        /// find
        User host = getAuthenticatedUser();
        Mission mission = resolveMission(request.missionId());
        Relic relic = resolveActiveRelic(request.relicId());


        /// validate
        validateNoDuplicateListing(host.getId(),request.missionId());

        hostListingValidator.validateListingCreation(request.playerCount(), request.maxPlayers(), mission, relic, request.refinementLevel(),request.missionSource());

        /// interact with the repository
        HostListing listing = buildListing(request, host, mission, relic);


        /// save and return RESPONSE
        return mapToResponse(hostListingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public JoinHostListingResponse joinListing(Long listingId) {

        HostListing listing = findListingById(listingId);

        hostListingValidator.validateListingCanReceiveJoinRequest(listing);

        return mapToJoinResponse(listing);
    }

    @Transactional
    public HostListingResponse updatePlayerCount(Long listingId, UpdatePlayerCountRequest request){
        HostListing listing = findListingById(listingId);

        User authenticatedUser = getAuthenticatedUser();

        hostListingValidator.validateListingCanChangePlayerCount(listing, authenticatedUser);

        hostListingValidator.validatePlayerCapacity(request.playerCount(), listing.getMaxPlayers());

        listing.setPlayerCount(request.playerCount());

        updateListingStatusFromPlayerCount(listing);

        return mapToResponse(listing);

    }

    /** PATCH */
    @Transactional
    public HostListingResponse assignMission(Long listingId, AssignMissionRequest request) {

        HostListing listing = findListingById(listingId);
        Mission mission = findMissionById(request.missionId());

        hostListingValidator.validateListingCanBeAssignedMission(getAuthenticatedUser(), mission, listing);


        listing.setMission(mission);

        return mapToResponse(listing);
    }




    /** DELETE/CLOSE */
    @Transactional
    public HostListingResponse closeListing(Long listingId) {
        HostListing listing = findListingById(listingId);

        hostListingValidator.validateListingCanBeClosed(listing, getAuthenticatedUser());

        listing.setStatus(HostListingStatus.CLOSED);

        return mapToResponse(listing);
    }

    @Transactional
    public int closeListingsForInactiveMissions() {
        List<HostListing> openListings =
                hostListingRepository.findByStatusNot(
                        HostListingStatus.CLOSED
                );

        List<HostListing> listingsToClose = openListings.stream()
                .filter(listing -> listing.getMission() != null && !listing.getMission().isActive())
                .peek(listing ->
                        listing.setStatus(HostListingStatus.CLOSED)
                )
                .toList();

        hostListingRepository.saveAll(listingsToClose);

        return listingsToClose.size();
    }



    
    /** GET */
    @Transactional(readOnly = true)
    public HostListingResponse getListingById(Long listingId) {
        return mapToResponse(findListingById(listingId));
    }


    /// get listing by host id
    @Transactional(readOnly = true)
    public List<HostListingResponse> getListingByHostId(Long hostId) {
        return hostListingRepository.findByHostId(hostId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /// get full listing by host id
    @Transactional(readOnly = true)
    public List<HostListingResponse> getFullListingByHostId(Long hostId) {
        return hostListingRepository.findByStatusAndHostId(HostListingStatus.FULL, hostId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /// get all listings
    @Transactional(readOnly = true)
    public List<HostListingResponse> getAllListings() {
        return hostListingRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HostListingResponse> getOpenListings() {
        return hostListingRepository
                .findByStatus(HostListingStatus.OPEN)
                .stream()
                .filter(listing -> listing.getMission() == null || listing.getMission().isActive())
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HostListingResponse> getOpenListingsByMission(Long missionId) {
        Mission mission = findMissionById(missionId);

        hostListingValidator.validateMissionIsActive(mission);

        return hostListingRepository
                .findByMissionIdAndStatus(
                        missionId,
                        HostListingStatus.OPEN
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }




    /** Private Helpers */


    
    private HostListing findListingById(Long listingId) {
        return hostListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Host listing not found with ID: " + listingId
                ));
    }

    private User findUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));
    }

    private Mission findMissionById(Long missionId){
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mission not found with ID: " + missionId
                ));
    }

    private Relic resolveActiveRelic(Long relicId) {
        if (relicId == null) {
            return null;
        }

        Relic relic = relicRepository.findById(relicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Relic not found with ID: " + relicId
                ));

        if (!relic.isActive()) {
            throw new InvalidRequestException(
                    "Cannot create a listing for an inactive relic"
            );
        }

        return relic;
    }

    private Mission resolveMission(Long missionId) {
        if (missionId == null) {
            return null;
        }
        Mission mission = findMissionById(missionId);
        hostListingValidator.validateMissionIsActive(mission);


        return mission;
    }



    /// Validation Helpers


    private void validateNoDuplicateListing(Long hostId, Long missionId){
        boolean exists = hostListingRepository.existsByHostIdAndMissionIdAndStatusNot(hostId, missionId, HostListingStatus.CLOSED);

        if (exists) {
            throw new DuplicateHostListingException(
                    hostId,
                    missionId
            );
        }
    }


    /// State Helpers

    private void updateListingStatusFromPlayerCount(HostListing listing) {
        if(listing.getPlayerCount() >= listing.getMaxPlayers()) {
            listing.setStatus(HostListingStatus.FULL);
            return;
        }
        listing.setStatus(HostListingStatus.OPEN);
    }

    private HostListing buildListing(CreateHostListingRequest request, User host, Mission mission, Relic relic){

        /// create a new Object
        HostListing listing = new HostListing();

        /// set
        listing.setHost(host);
        listing.setMission(mission);
        listing.setMissionSource(request.missionSource());
        listing.setRelic(relic);
        listing.setRefinementLevel(request.refinementLevel());
        listing.setPlayerCount(request.playerCount());
        listing.setMaxPlayers(request.maxPlayers());
        listing.setExpectedDurationMinutes(request.expectedDurationMinutes());

        updateListingStatusFromPlayerCount(listing);

        return listing;
    }


    private RelicEra determineRelicEra(String relicName) {
        String prefix = relicName.trim()
                .split("\\s+")[0]
                .toUpperCase();

        try {
            return RelicEra.valueOf(prefix);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "Unsupported relic era in name: " + relicName
            );
        }
    }

    /// ToResponseMappers

    private JoinHostListingResponse mapToJoinResponse(HostListing listing) {
        String hostName =
                listing.getHost().getWarframeUserName();

        String whisperCommand = buildWhisperCommand(listing);

        return new JoinHostListingResponse(
                listing.getId(),
                hostName,
                whisperCommand,
                listing.getPlayerCount(),
                listing.getMaxPlayers()
        );
    }



    private String buildWhisperCommand(
            HostListing listing
    ) {
        String hostName =
                listing.getHost().getWarframeUserName();

        if (listing.getMissionSource() == MissionSource.ARBITRATION) {
            return "/w "
                    + hostName
                    + " Hi, I would like to join your Arbitration squad.";
        }

        if (listing.getMission() == null
                && listing.getRelic() != null) {

            return "/w "
                    + hostName
                    + " Hi, I would like to join your "
                    + listing.getRelic().getName()
                    + " relic squad.";
        }

        Mission mission = listing.getMission();

        String missionDetails =
                mission.getNode()
                        + " "
                        + mission.getMissionType();

        if (listing.getRelic() != null) {
            missionDetails += " using "
                    + listing.getRelic().getName();
        }

        return "/w "
                + hostName
                + " Hi, I would like to join your squad for "
                + missionDetails;
    }

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



    /// User Authentification
    private User getAuthenticatedUser(){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())){
            throw new InvalidListingStateException("User must be authenticated");
        }
        return userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}