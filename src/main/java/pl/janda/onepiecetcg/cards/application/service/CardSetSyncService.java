package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.repository.CardSetRepository;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardSetRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSetSyncService implements CardSetSyncUseCase {

    private final CardSetRepository cardSetRepository;

    private final OnePieceCardSetRepository onePieceCardSetRepository;


    @Transactional
    @Override
    public boolean syncCardSets() {
        var sourceSets = onePieceCardSetRepository.findAll();
        if (sourceSets.isEmpty()) {
            log.warn("No card sets found in either catalog source table, skipping sync");
            return false;
        }

        var fetched = sourceSets.stream()
                .map(source -> CardSet.builder()
                        .setId(source.getSetId())
                        .setName(source.getLabel())
                        .released(source.isReleased())
                        .releaseDate(source.getReleaseDate())
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
                    return existing == null
                            || !Objects.equals(cardSet.getSetName(), existing.getSetName())
                            || cardSet.isReleased() != existing.isReleased()
                            || !Objects.equals(cardSet.getReleaseDate(), existing.getReleaseDate());
                })
                .toList();

        var removedLeaks = existingById.values().stream()
                .filter(existing -> !existing.isReleased())
                .filter(existing -> fetched.stream().noneMatch(source -> source.getSetId().equals(existing.getSetId())))
                .toList();

        if (changedSets.isEmpty() && removedLeaks.isEmpty()) {
            log.info("card_sets is already up to date with the source tables, skipping sync");
            return false;
        }

        var now = LocalDateTime.now();
        fetched.forEach(cardSet -> cardSet.setLastSyncedAt(now));
        if (!changedSets.isEmpty()) {
            cardSetRepository.saveAll(fetched);
        }
        if (!removedLeaks.isEmpty()) {
            cardSetRepository.deleteAll(removedLeaks);
        }
        log.info("Synced {} card sets from source tables ({} new: {}, {} changed, {} expired leaks removed)",
                fetched.size(), newSets.size(), newSets.stream().map(CardSet::getSetId).toList(),
                changedSets.size(), removedLeaks.size());
        return !newSets.isEmpty() || !removedLeaks.isEmpty();
    }
}
