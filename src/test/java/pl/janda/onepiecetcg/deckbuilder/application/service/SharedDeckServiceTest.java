package pl.janda.onepiecetcg.deckbuilder.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.deckbuilder.application.model.CreateSharedDeckCardCommand;
import pl.janda.onepiecetcg.deckbuilder.application.model.CreateSharedDeckCommand;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeck;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeckCard;
import pl.janda.onepiecetcg.deckbuilder.application.repository.SharedDeckRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedDeckServiceTest {

    @Mock
    private SharedDeckRepository sharedDeckRepository;

    @Mock
    private CardCatalogUseCase cardCatalogUseCase;

    @Test
    void createSharedDeck_persistsStableCardNumbersAndReturnsResolvedDetails() {
        var leader = SetCard.builder().id(10L).cardSetId("OP01-001").cardType("Leader").build();
        var deckCard = SetCard.builder().id(20L).cardSetId("OP01-006").cardType("Character").build();
        var command = new CreateSharedDeckCommand(
                "  Red Luffy  ",
                " OP01-001 ",
                List.of(new CreateSharedDeckCardCommand(" OP01-006 ", 4)));
        var service = new SharedDeckService(sharedDeckRepository, cardCatalogUseCase);

        when(cardCatalogUseCase.getRepresentativeCardsByCardCodes(List.of("OP01-001", "OP01-006")))
                .thenReturn(List.of(leader, deckCard));
        when(sharedDeckRepository.save(any(SharedDeck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.createSharedDeck(command);

        var captor = ArgumentCaptor.forClass(SharedDeck.class);
        verify(sharedDeckRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getShareCode()).hasSize(10).matches("[1-9A-HJ-NP-Za-km-z]+");
        assertThat(saved.getName()).isEqualTo("Red Luffy");
        assertThat(saved.getLeaderCardNumber()).isEqualTo("OP01-001");
        assertThat(saved.getCards())
                .extracting(SharedDeckCard::getCardNumber, SharedDeckCard::getQuantity)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("OP01-006", 4));
        assertThat(details.leader()).isSameAs(leader);
        assertThat(details.cards().getFirst().card()).isSameAs(deckCard);
    }

    @Test
    void getSharedDeck_hydratesAllCardReferencesInOneCatalogCall() {
        var sharedDeck = SharedDeck.builder()
                .shareCode("K7mQ2xP9Wa")
                .name("Red Luffy")
                .leaderCardNumber("OP01-001")
                .cards(List.of(
                        SharedDeckCard.builder().cardNumber("OP01-006").quantity(4).build(),
                        SharedDeckCard.builder().cardNumber("OP02-004").quantity(3).build()))
                .createdAt(Instant.parse("2026-08-04T12:00:00Z"))
                .build();
        var leader = SetCard.builder().cardSetId("OP01-001").cardType("Leader").build();
        var first = SetCard.builder().cardSetId("OP01-006").cardType("Character").build();
        var second = SetCard.builder().cardSetId("OP02-004").cardType("Event").build();
        var service = new SharedDeckService(sharedDeckRepository, cardCatalogUseCase);

        when(sharedDeckRepository.findByShareCode("K7mQ2xP9Wa")).thenReturn(Optional.of(sharedDeck));
        when(cardCatalogUseCase.getRepresentativeCardsByCardCodes(
                List.of("OP01-001", "OP01-006", "OP02-004")))
                .thenReturn(List.of(first, leader, second));

        var details = service.getSharedDeck("K7mQ2xP9Wa");

        assertThat(details.leader()).isSameAs(leader);
        assertThat(details.cards()).extracting(card -> card.card().getCardSetId())
                .containsExactly("OP01-006", "OP02-004");
        verify(cardCatalogUseCase).getRepresentativeCardsByCardCodes(
                List.of("OP01-001", "OP01-006", "OP02-004"));
    }

    @Test
    void createSharedDeck_rejectsLeaderInRegularDeckCards() {
        var leader = SetCard.builder().cardSetId("OP01-001").cardType("Leader").build();
        var command = new CreateSharedDeckCommand(
                "Invalid",
                null,
                List.of(new CreateSharedDeckCardCommand("OP01-001", 1)));
        var service = new SharedDeckService(sharedDeckRepository, cardCatalogUseCase);

        when(cardCatalogUseCase.getRepresentativeCardsByCardCodes(List.of("OP01-001")))
                .thenReturn(List.of(leader));

        assertThatThrownBy(() -> service.createSharedDeck(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Leader cannot be included");
        verify(sharedDeckRepository, never()).save(any());
    }

    @Test
    void createSharedDeck_rejectsMoreThanFiftyCardsBeforeCatalogLookup() {
        var command = new CreateSharedDeckCommand(
                "Invalid",
                null,
                List.of(
                        new CreateSharedDeckCardCommand("OP01-001", 4),
                        new CreateSharedDeckCardCommand("OP01-002", 4),
                        new CreateSharedDeckCardCommand("OP01-003", 4),
                        new CreateSharedDeckCardCommand("OP01-004", 4),
                        new CreateSharedDeckCardCommand("OP01-005", 4),
                        new CreateSharedDeckCardCommand("OP01-006", 4),
                        new CreateSharedDeckCardCommand("OP01-007", 4),
                        new CreateSharedDeckCardCommand("OP01-008", 4),
                        new CreateSharedDeckCardCommand("OP01-009", 4),
                        new CreateSharedDeckCardCommand("OP01-010", 4),
                        new CreateSharedDeckCardCommand("OP01-011", 4),
                        new CreateSharedDeckCardCommand("OP01-012", 4),
                        new CreateSharedDeckCardCommand("OP01-013", 3)));
        var service = new SharedDeckService(sharedDeckRepository, cardCatalogUseCase);

        assertThatThrownBy(() -> service.createSharedDeck(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 50");
        verify(cardCatalogUseCase, never()).getRepresentativeCardsByCardCodes(any());
    }

    @Test
    void getSharedDeck_throwsNotFoundForUnknownCode() {
        var service = new SharedDeckService(sharedDeckRepository, cardCatalogUseCase);
        when(sharedDeckRepository.findByShareCode("unknown123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSharedDeck("unknown123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Shared deck not found with code: unknown123");
    }
}
