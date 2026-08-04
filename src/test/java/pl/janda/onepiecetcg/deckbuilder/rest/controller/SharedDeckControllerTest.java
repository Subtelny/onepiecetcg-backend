package pl.janda.onepiecetcg.deckbuilder.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.*;
import pl.janda.onepiecetcg.deckbuilder.application.model.*;
import pl.janda.onepiecetcg.deckbuilder.application.port.in.SharedDeckUseCase;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = OnePieceTcgApplication.class)
@AutoConfigureMockMvc
class SharedDeckControllerTest extends PostgresSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SharedDeckUseCase sharedDeckUseCase;

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
    void createSharedDeck_returnsShortPathAndLocation() throws Exception {
        var deck = sharedDeck();
        when(sharedDeckUseCase.createSharedDeck(any(CreateSharedDeckCommand.class)))
                .thenReturn(new SharedDeckDetails(deck, null, List.of()));

        mockMvc.perform(post("/api/deckbuilder/shared-decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Red Luffy",
                                  "leaderCardNumber": "OP01-001",
                                  "cards": [{"cardNumber": "OP01-006", "quantity": 4}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/deckbuilder/shared-decks/K7mQ2xP9Wa"))
                .andExpect(jsonPath("$.code").value("K7mQ2xP9Wa"))
                .andExpect(jsonPath("$.path").value("/d/K7mQ2xP9Wa"));

        var captor = org.mockito.ArgumentCaptor.forClass(CreateSharedDeckCommand.class);
        verify(sharedDeckUseCase).createSharedDeck(captor.capture());
        assertThat(captor.getValue().leaderCardNumber()).isEqualTo("OP01-001");
        assertThat(captor.getValue().cards().getFirst().cardNumber()).isEqualTo("OP01-006");
    }

    @Test
    void createSharedDeck_rejectsDuplicateCardNumbers() throws Exception {
        mockMvc.perform(post("/api/deckbuilder/shared-decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid",
                                  "cards": [
                                    {"cardNumber": "OP01-006", "quantity": 2},
                                    {"cardNumber": "OP01-006", "quantity": 2}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cardNumbersUnique").exists());

        verifyNoInteractions(sharedDeckUseCase);
    }

    @Test
    void createSharedDeck_rejectsTotalQuantityAboveFifty() throws Exception {
        var cards = new StringBuilder();
        for (var i = 1; i <= 13; i++) {
            if (!cards.isEmpty()) {
                cards.append(',');
            }
            cards.append("{\"cardNumber\":\"OP01-")
                    .append(String.format("%03d", i))
                    .append("\",\"quantity\":4}");
        }

        mockMvc.perform(post("/api/deckbuilder/shared-decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Invalid\",\"cards\":[" + cards + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.totalCardCountValid").exists());

        verifyNoInteractions(sharedDeckUseCase);
    }

    @Test
    void getSharedDeck_returnsHydratedCardDetailsAndIsoTimestamp() throws Exception {
        var deck = sharedDeck();
        var leader = SetCard.builder()
                .id(1L)
                .cardSetId("OP01-001")
                .cardName("Monkey.D.Luffy")
                .cardType("Leader")
                .build();
        var card = SetCard.builder()
                .id(2L)
                .cardSetId("OP01-006")
                .cardName("Otama")
                .cardType("Character")
                .build();
        var entry = SharedDeckCard.builder().cardNumber("OP01-006").quantity(4).build();
        when(sharedDeckUseCase.getSharedDeck("K7mQ2xP9Wa"))
                .thenReturn(new SharedDeckDetails(
                        deck,
                        leader,
                        List.of(new SharedDeckCardDetails(entry, card))));

        mockMvc.perform(get("/api/deckbuilder/shared-decks/K7mQ2xP9Wa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("K7mQ2xP9Wa"))
                .andExpect(jsonPath("$.name").value("Red Luffy"))
                .andExpect(jsonPath("$.leader.cardNumber").value("OP01-001"))
                .andExpect(jsonPath("$.cards[0].card.cardNumber").value("OP01-006"))
                .andExpect(jsonPath("$.cards[0].quantity").value(4))
                .andExpect(jsonPath("$.createdAt").value("2026-08-04T12:00:00Z"));
    }

    private SharedDeck sharedDeck() {
        return SharedDeck.builder()
                .shareCode("K7mQ2xP9Wa")
                .name("Red Luffy")
                .leaderCardNumber("OP01-001")
                .cards(List.of())
                .createdAt(Instant.parse("2026-08-04T12:00:00Z"))
                .build();
    }
}
