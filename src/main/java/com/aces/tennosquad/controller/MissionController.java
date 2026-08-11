package com.aces.tennosquad.controller;

import com.aces.tennosquad.dto.mission.MissionResponse;
import com.aces.tennosquad.dto.mission.SyncMissionRequest;
import com.aces.tennosquad.model.enums.MissionType;
import com.aces.tennosquad.service.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;
    
    /** POST */

    @PostMapping
    public ResponseEntity<MissionResponse> syncMission(
            @Valid @RequestBody SyncMissionRequest request
    ) {
        MissionResponse response =
                missionService.syncMission(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    /** PATCH */


    /** GET */
    
    @GetMapping
    public ResponseEntity<List<MissionResponse>> getAllMissions() {
        return ResponseEntity.ok(
                missionService.getAllMissions()
        );
    }

    @GetMapping("/{missionId}")
    public ResponseEntity<MissionResponse> getMissionById(
            @PathVariable Long missionId
    ) {
        return ResponseEntity.ok(
                missionService.getMissionById(missionId)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<List<MissionResponse>> getActiveMissions() {
        return ResponseEntity.ok(
                missionService.getActiveMissions()
        );
    }

    @GetMapping("/active/type/{missionType}")
    public ResponseEntity<List<MissionResponse>> getActiveMissionsByType(
            @PathVariable MissionType missionType
    ) {
        return ResponseEntity.ok(
                missionService.getActiveMissionsByType(missionType)
        );
    }


}