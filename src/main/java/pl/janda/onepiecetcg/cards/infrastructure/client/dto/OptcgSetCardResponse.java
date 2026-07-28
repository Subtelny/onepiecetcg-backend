package pl.janda.onepiecetcg.cards.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OptcgSetCardResponse(
        @JsonProperty("inventory_price") Double inventoryPrice,
        @JsonProperty("market_price") Double marketPrice,
        @JsonProperty("card_name") String cardName,
        @JsonProperty("set_name") String setName,
        @JsonProperty("card_text") String cardText,
        @JsonProperty("set_id") String setId,
        @JsonProperty("rarity") String rarity,
        @JsonProperty("card_set_id") String cardSetId,
        @JsonProperty("card_color") String cardColor,
        @JsonProperty("card_type") String cardType,
        @JsonProperty("life") String life,
        @JsonProperty("card_cost") String cardCost,
        @JsonProperty("card_power") String cardPower,
        @JsonProperty("sub_types") String subTypes,
        @JsonProperty("counter_amount") Integer counterAmount,
        @JsonProperty("attribute") String attribute,
        @JsonProperty("date_scraped") String dateScraped,
        @JsonProperty("card_image_id") String cardImageId,
        @JsonProperty("card_image") String cardImage
) {
}
