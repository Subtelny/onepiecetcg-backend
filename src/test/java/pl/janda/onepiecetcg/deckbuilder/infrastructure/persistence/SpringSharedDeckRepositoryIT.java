package pl.janda.onepiecetcg.deckbuilder.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.port.in.*;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeck;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeckCard;
import pl.janda.onepiecetcg.deckbuilder.application.repository.SharedDeckRepository;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OnePieceTcgApplication.class)
class SpringSharedDeckRepositoryIT extends PostgresSpringBootTest {

    @Autowired
    private SharedDeckRepository sharedDeckRepository;

    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;

    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;

    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;

    @Test
    void saveAndFindByShareCode_roundTripsSnapshotAndElementCollection() {
        var createdAt = Instant.parse("2026-08-04T12:00:00Z");
        var sharedDeck = SharedDeck.builder()
                .shareCode("DbPersist1")
                .name("Persistent deck")
                .leaderCardNumber("OP01-001")
                .cards(List.of(
                        SharedDeckCard.builder().cardNumber("OP01-006").quantity(4).build(),
                        SharedDeckCard.builder().cardNumber("OP02-004").quantity(3).build()))
                .createdAt(createdAt)
                .build();

        sharedDeckRepository.save(sharedDeck);

        var reloaded = sharedDeckRepository.findByShareCode("DbPersist1");
        assertThat(reloaded).isPresent();
        assertThat(reloaded.orElseThrow().getName()).isEqualTo("Persistent deck");
        assertThat(reloaded.orElseThrow().getLeaderCardNumber()).isEqualTo("OP01-001");
        assertThat(reloaded.orElseThrow().getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.orElseThrow().getCards())
                .extracting(SharedDeckCard::getCardNumber, SharedDeckCard::getQuantity)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("OP01-006", 4),
                        org.assertj.core.groups.Tuple.tuple("OP02-004", 3));
    }
}
