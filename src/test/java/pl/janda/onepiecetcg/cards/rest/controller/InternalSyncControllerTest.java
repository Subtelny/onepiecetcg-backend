package pl.janda.onepiecetcg.cards.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OnePieceTcgApplication.class)
@AutoConfigureMockMvc
class InternalSyncControllerTest extends PostgresSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;

    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;

    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;

    @MockitoBean
    private MatchupSyncUseCase matchupSyncUseCase;

    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @Test
    void recalculateMatchups_forwardsDatasetAndReturnsSuccess() throws Exception {
        when(matchupSyncUseCase.recalculateMatchups("Special_Queue")).thenReturn(true);

        mockMvc.perform(post("/api/internal/sync/matchups/recalculate")
                        .header("X-API-Key", "dev-only-change-me")
                        .param("dataset", "Special_Queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggered").value(true))
                .andExpect(jsonPath("$.message").value("Matchup dataset recalculated: Special_Queue"));

        verify(matchupSyncUseCase).recalculateMatchups("Special_Queue");
    }

    @Test
    void recalculateMatchups_rejectsMissingApiKey() throws Exception {
        mockMvc.perform(post("/api/internal/sync/matchups/recalculate")
                        .param("dataset", "Special_Queue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recalculateMatchups_returnsNotFoundForUnknownDataset() throws Exception {
        when(matchupSyncUseCase.recalculateMatchups("unknown"))
                .thenThrow(new IllegalArgumentException("Matchup dataset not found: unknown"));

        mockMvc.perform(post("/api/internal/sync/matchups/recalculate")
                        .header("X-API-Key", "dev-only-change-me")
                        .param("dataset", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Matchup dataset not found: unknown"));
    }
}
