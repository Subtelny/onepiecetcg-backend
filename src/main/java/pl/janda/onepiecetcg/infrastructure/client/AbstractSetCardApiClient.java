package pl.janda.onepiecetcg.infrastructure.client;

import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.infrastructure.client.dto.OptcgSetCardResponse;

public abstract class AbstractSetCardApiClient extends AbstractOptcgApiClient {

    protected AbstractSetCardApiClient(RestClient.Builder restClientBuilder, String baseUrl) {
        super(restClientBuilder, baseUrl);
    }

    protected SetCard toSetCard(OptcgSetCardResponse r, boolean promo) {
        return SetCard.builder()
                .cardSetId(r.cardSetId())
                .cardPrefix(extractPrefix(r.cardSetId()))
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
                .promo(promo)
                .build();
    }

    private static String extractPrefix(String cardSetId) {
        if (cardSetId == null) {
            return null;
        }
        int idx = cardSetId.indexOf('-');
        if (idx <= 0) {
            return null;
        }
        return cardSetId.substring(0, idx);
    }
}
