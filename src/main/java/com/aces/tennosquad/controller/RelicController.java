package com.aces.tennosquad.controller;


import com.aces.tennosquad.dto.relic.CreateRelicRequest;
import com.aces.tennosquad.dto.relic.RelicResponse;
import com.aces.tennosquad.service.RelicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relics")
@RequiredArgsConstructor
public class RelicController {

    private final RelicService relicService;

    /** POST */
    @PostMapping
    public ResponseEntity<RelicResponse> createRelic(@Valid @RequestBody CreateRelicRequest request){
        RelicResponse relicResponse = relicService.createRelic(request);

        return ResponseEntity
                .ok()
                .body(relicResponse);
    }


    /** GET */
    @GetMapping
    public ResponseEntity<List<RelicResponse>> getAllRelic(){
        List<RelicResponse> relicResponses = relicService.getAllRelics();
        return ResponseEntity.ok(relicResponses);
    }

    /** DELETE */


}
