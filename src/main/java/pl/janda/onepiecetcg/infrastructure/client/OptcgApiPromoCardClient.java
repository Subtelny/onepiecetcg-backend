package pl.janda.onepiecetcg.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.application.client.PromoCardApiClient;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.infrastructure.client.dto.OptcgSetCardResponse;

import java.util.Arrays;
import java.util.List;

@Component
public class OptcgApiPromoCardClient implements PromoCardApiClient {

    private final RestClient restClient;

    public OptcgApiPromoCardClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://www.optcgapi.com/api")
                .build();
    }

    @Override
    public List<SetCard> fetchAllPromoCards() {
        var response = restClient.get()
                .uri("/allPromos/")
                .retrieve()
                .body(OptcgSetCardResponse[].class);

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .map(this::toSetCard)
                .toList();
    }

    private SetCard toSetCard(OptcgSetCardResponse r) {
        return SetCard.builder()
                .cardSetId(r.cardSetId())
                .cardName(r.cardName())
                .setId(r.setId())
                .setName(r.setName())
                .cardText(r.cardText())
                .rarity(r.rarity())
                .cardColor(r.cardColor())
                .cardType(r.cardType())
                .life(r.life())
                .cardCost(r.cardCost())
                .cardPower(r.cardPower())
                .subTypes(r.subTypes())
                .counterAmount(r.counterAmount())
                .attribute(r.attribute())
                .dateScraped(r.dateScraped())
                .cardImageId(r.cardImageId())
                .cardImage(r.cardImage())
                .inventoryPrice(r.inventoryPrice())
                .marketPrice(r.marketPrice())
                .promo(true)
                .build();
    }
}
