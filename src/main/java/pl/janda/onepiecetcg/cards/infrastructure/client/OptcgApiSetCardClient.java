package pl.janda.onepiecetcg.cards.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.cards.application.client.SetCardApiClient;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.OptcgSetCardResponse;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OptcgApiSetCardClient extends AbstractSetCardApiClient implements SetCardApiClient {

    public OptcgApiSetCardClient(RestClient.Builder restClientBuilder,
                                  @Value("${optcgapi.base-url}") String baseUrl) {
        super(restClientBuilder, baseUrl);
    }

    /**
     * Accumulates straight into one list rather than keeping the three per-endpoint lists alive to
     * concatenate at the end. That earlier shape held four full copies of the catalog on the heap
     * simultaneously, which is the sync's peak allocation and the app runs in a 1 GB container.
     * Each endpoint's list is unreachable again as soon as it has been drained.
     */
    @Override
    public List<SetCard> fetchAllSetCards() {
        log.info("Starting fetch of all set cards from optcgapi.com");
        var allCards = new ArrayList<SetCard>();

        log.info("Fetching regular set cards from /allSetCards/");
        allCards.addAll(fetchAndMap("/allSetCards/", OptcgSetCardResponse[].class, r -> toSetCard(r, false)));
        var setCardCount = allCards.size();
        log.info("Fetched {} regular set cards", setCardCount);

        log.info("Fetching starter deck cards from /allSTCards/");
        allCards.addAll(fetchAndMap("/allSTCards/", OptcgSetCardResponse[].class, r -> toSetCard(r, false)));
        var stCardCount = allCards.size() - setCardCount;
        log.info("Fetched {} starter deck cards", stCardCount);

        log.info("Fetching promo cards from /allPromos/");
        allCards.addAll(fetchAndMap("/allPromos/", OptcgSetCardResponse[].class, r -> toSetCard(r, true)));
        var promoCardCount = allCards.size() - setCardCount - stCardCount;
        log.info("Fetched {} promo cards", promoCardCount);

        log.info("Combined total: {} cards (set: {}, starter: {}, promo: {})",
                allCards.size(), setCardCount, stCardCount, promoCardCount);

        return allCards;
    }
}
