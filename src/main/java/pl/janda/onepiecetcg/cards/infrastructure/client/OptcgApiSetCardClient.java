package pl.janda.onepiecetcg.cards.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.cards.application.client.SetCardApiClient;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.OptcgSetCardResponse;

import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class OptcgApiSetCardClient extends AbstractSetCardApiClient implements SetCardApiClient {

    public OptcgApiSetCardClient(RestClient.Builder restClientBuilder,
                                  @Value("${optcgapi.base-url}") String baseUrl) {
        super(restClientBuilder, baseUrl);
    }

    @Override
    public List<SetCard> fetchAllSetCards() {
        log.info("Starting fetch of all set cards from optcgapi.com");

        log.info("Fetching regular set cards from /allSetCards/");
        var setCards = fetchAndMap("/allSetCards/", OptcgSetCardResponse[].class, r -> toSetCard(r, false));
        log.info("Fetched {} regular set cards", setCards.size());

        log.info("Fetching starter deck cards from /allSTCards/");
        var stCards = fetchAndMap("/allSTCards/", OptcgSetCardResponse[].class, r -> toSetCard(r, false));
        log.info("Fetched {} starter deck cards", stCards.size());

        log.info("Fetching promo cards from /allPromos/");
        var promoCards = fetchAndMap("/allPromos/", OptcgSetCardResponse[].class, r -> toSetCard(r, true));
        log.info("Fetched {} promo cards", promoCards.size());

        var allCards = Stream.concat(Stream.concat(setCards.stream(), stCards.stream()), promoCards.stream()).toList();
        log.info("Combined total: {} cards (set: {}, starter: {}, promo: {})",
                allCards.size(), setCards.size(), stCards.size(), promoCards.size());

        return allCards;
    }
}
