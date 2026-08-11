package com.aces.tennosquad.model;

import com.aces.tennosquad.model.enums.RelicEra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "relics")
@Getter
@Setter
@NoArgsConstructor
public class Relic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private RelicEra relicEra;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(
            mappedBy = "relic",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RelicReward> rewards = new ArrayList<>();

    public void addReward(RelicReward reward) {
        rewards.add(reward);
        reward.setRelic(this);
    }

    public void removeReward(RelicReward reward) {
        rewards.remove(reward);
        reward.setRelic(null);
    }
}