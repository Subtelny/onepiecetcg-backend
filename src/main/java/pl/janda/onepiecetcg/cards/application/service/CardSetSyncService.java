package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.repository.CardSetRepository;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardSetRepository;

import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSetSyncService {

    private final CardSetRepository cardSetRepository;

    private final OnePieceCardSetRepository onePieceCardSetRepository;

    /**
     * Synchronizes new sets and label changes from the source table.
     *
     * @return true if at least one new set was persisted, false otherwise
     */
    @Transactional
    public boolean syncCardSets() {
        var sourceSets = onePieceCardSetRepository.findAll();
        if (sourceSets.isEmpty()) {
            log.warn("No card sets found in onepiece_card_sets, skipping sync");
            return false;
        }

        var fetched = sourceSets.stream()
                .map(source -> CardSet.builder()
                        .setId(source.getSetId())
                        .setName(source.getLabel())
                        .build())
                .toList();
        var existingById = cardSetRepository.findAll().stream()
                .collect(Collectors.toMap(CardSet::getSetId, Function.identity()));

        var newSets = fetched.stream()
                .filter(cardSet -> !existingById.containsKey(cardSet.getSetId()))
                .toList();
        var changedSets = fetched.stream()
                .filter(cardSet -> {
                    var existing = existingById.get(cardSet.getSetId());
                    return existing == null || !cardSet.getSetName().equals(existing.getSetName());
                })
                .toList();

        if (changedSets.isEmpty()) {
            log.info("card_sets is already up to date with onepiece_card_sets, skipping sync");
            return false;
        }

        var now = LocalDateTime.now();
        fetched.forEach(cardSet -> cardSet.setLastSyncedAt(now));
        cardSetRepository.saveAll(fetched);
        log.info("Synced {} card sets from onepiece_card_sets ({} new: {}, {} changed)",
                fetched.size(), newSets.size(), newSets.stream().map(CardSet::getSetId).toList(),
                changedSets.size());
        return !newSets.isEmpty();
    }
}
