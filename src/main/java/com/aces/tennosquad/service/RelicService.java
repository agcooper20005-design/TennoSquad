package com.aces.tennosquad.service;

import com.aces.tennosquad.dto.relic.CreateRelicRequest;
import com.aces.tennosquad.dto.relic.ImportedRelicReward;
import com.aces.tennosquad.dto.relic.RelicResponse;
import com.aces.tennosquad.dto.relic.RelicRewardResponse;
import com.aces.tennosquad.exception.DuplicateResourceException;
import com.aces.tennosquad.exception.InvalidRequestException;
import com.aces.tennosquad.exception.ResourceNotFoundException;
import com.aces.tennosquad.model.Relic;
import com.aces.tennosquad.model.RelicReward;
import com.aces.tennosquad.repository.RelicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RelicService {

    private final RelicRepository relicRepository;

    @Transactional
    public RelicResponse createRelic(CreateRelicRequest request) {

        String normalizedName = request.name().trim();

        if (relicRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException(
                    "Relic already exists with name: " + normalizedName
            );
        }

        Relic relic = new Relic();
        relic.setName(normalizedName);
        relic.setActive(request.active());
        relic.setRelicEra(request.era());

        Relic savedRelic = relicRepository.save(relic);

        return mapToResponse(savedRelic);
    }

    @Transactional(readOnly = true)
    public RelicResponse getRelicById(Long relicId) {
        return mapToResponse(findRelicById(relicId));
    }

    @Transactional(readOnly = true)
    public RelicResponse getRelicByName(String relicName) {

        String normalizedName = relicName.trim();

        Relic relic = relicRepository
                .findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Relic not found with name: " + normalizedName
                ));

        return mapToResponse(relic);
    }

    @Transactional(readOnly = true)
    public List<RelicResponse> getAllRelics() {
        return relicRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RelicResponse> getActiveRelics() {
        return relicRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public RelicResponse activateRelic(Long relicId) {

        Relic relic = findRelicById(relicId);

        relic.setActive(true);

        return mapToResponse(relic);
    }

    @Transactional
    public RelicResponse deactivateRelic(Long relicId) {

        Relic relic = findRelicById(relicId);

        relic.setActive(false);

        return mapToResponse(relic);
    }

    /**
     * Replaces every existing reward with the newest imported reward data.
     */
    @Transactional
    public RelicResponse replaceRewards(
            Long relicId,
            List<ImportedRelicReward> importedRewards
    ) {
        if (importedRewards == null || importedRewards.isEmpty()) {
            throw new InvalidRequestException(
                    "Imported reward list cannot be empty"
            );
        }
        Relic relic = findRelicById(relicId);

        relic.getRewards().clear();

        List<RelicReward> newRewards = importedRewards.stream()
                .map(importedReward ->
                        createRewardEntity(relic, importedReward)
                )
                .toList();

        relic.getRewards().addAll(newRewards);

        return mapToResponse(relic);
    }

    private Relic findRelicById(Long relicId) {
        return relicRepository.findById(relicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Relic not found with ID: " + relicId
                ));
    }

    private RelicReward createRewardEntity(
            Relic relic,
            ImportedRelicReward importedReward
    ) {
        RelicReward reward = new RelicReward();

        reward.setRelic(relic);
        reward.setItemName(importedReward.itemName().trim());
        reward.setRarity(importedReward.rarity());
        reward.setChance(importedReward.chance());

        return reward;
    }

    private RelicResponse mapToResponse(Relic relic) {

        List<RelicRewardResponse> rewards = relic.getRewards()
                .stream()
                .map(this::mapRewardToResponse)
                .toList();

        return new RelicResponse(
                relic.getId(),
                relic.getName(),
                relic.getRelicEra(),
                relic.isActive(),
                rewards
        );
    }

    private RelicRewardResponse mapRewardToResponse(
            RelicReward reward
    ) {
        return new RelicRewardResponse(
                reward.getId(),
                reward.getItemName(),
                reward.getRarity(),
                reward.getChance()
        );
    }
}