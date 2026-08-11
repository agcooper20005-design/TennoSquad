package com.aces.tennosquad.model;

import com.aces.tennosquad.model.enums.Faction;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.MissionType;
import com.aces.tennosquad.model.enums.RelicEra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionType missionType;

    @Column(nullable = false)
    private String node;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Faction faction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionSource missionSource;

    @Column(nullable = false)
    private boolean steelPath;

    @Column(nullable = false)
    private boolean fissure;

    @Enumerated(EnumType.STRING)
    private RelicEra fissureEra;

    @Column(unique = true)
    private String externalMissionId;

    @Column(nullable = false)
    private boolean active;

    private Instant expiry;

    @PrePersist
    @PreUpdate
    public void updateActiveStatus() {
        active = expiry == null || expiry.isAfter(Instant.now());
    }

}