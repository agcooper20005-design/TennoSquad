package com.aces.tennosquad.repository;

import com.aces.tennosquad.model.HostListing;
import com.aces.tennosquad.model.enums.HostListingStatus;
import com.aces.tennosquad.model.enums.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostListingRepository extends JpaRepository<HostListing, Long> {

    List<HostListing> findByStatus(HostListingStatus status);

    List<HostListing> findByStatusAndHostId(HostListingStatus status, Long hostId);

    List<HostListing> findByStatusNot(HostListingStatus status);

    List<HostListing> findByHostId(Long hostId);

    List<HostListing> findByMissionId(Long missionId);

    List<HostListing> findByRelicId(Long relicId);

    List<HostListing> findByMissionMissionType(MissionType missionType);

    List<HostListing> findByStatusAndMissionMissionType(
            HostListingStatus status,
            MissionType missionType
    );

    List<HostListing> findByMissionIdAndStatus(
            Long missionId,
            HostListingStatus status
    );

    boolean existsByHostIdAndMissionIdAndStatusNot(Long hostId, Long missionId, HostListingStatus status);


}