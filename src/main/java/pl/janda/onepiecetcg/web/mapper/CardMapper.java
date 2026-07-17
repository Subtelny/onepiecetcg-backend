package pl.janda.onepiecetcg.web.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.web.dto.CardDto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CardMapper {

    public CardDto toDto(SetCard card) {
        if (card == null) {
            return null;
        }

        return CardDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
                .type(parseCardType(card.getCardType()))
                .color(parseColors(card.getCardColor()))
                .cost(parseIntSafe(card.getCardCost()))
                .power(parseIntSafe(card.getCardPower()))
                .counter(card.getCounterAmount())
                .attribute(card.getAttribute())
                .effect(card.getCardText())
                .rarity(card.getRarity())
                .cardNumber(card.getCardSetId())
                .imageUrl(card.getCardImage())
                .build();
    }

    public List<CardDto> toDtoList(List<SetCard> cards) {
        return cards != null ?
                cards.stream().map(this::toDto).collect(Collectors.toList()) : List.of();
    }

    private String parseCardType(String cardType) {
        if (cardType == null) {
            return null;
        }
        try {
            return CardType.valueOf(cardType.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> parseColors(String cardColor) {
        if (cardColor == null) {
            return List.of();
        }
        return Arrays.stream(cardColor.split("\\s+"))
                .map(String::toUpperCase)
                .filter(token -> Arrays.stream(CardColor.values()).anyMatch(c -> c.name().equals(token)))
                .toList();
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
