package pl.janda.onepiecetcg.web.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardErrata;
import pl.janda.onepiecetcg.application.model.CardFaq;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardSummary;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.web.dto.CardDto;
import pl.janda.onepiecetcg.web.dto.CardErrataEntryDto;
import pl.janda.onepiecetcg.web.dto.CardFaqEntryDto;
import pl.janda.onepiecetcg.web.dto.CardFilterOptionsDto;
import pl.janda.onepiecetcg.web.dto.CardSetOptionDto;
import pl.janda.onepiecetcg.web.dto.CardSummaryDto;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CardMapper {

    public CardDto toDto(SetCard card) {
        return toDto(card, List.of(), List.of());
    }

    public CardDto toDto(SetCard card, List<CardErrata> errataHistory, List<CardFaq> faqHistory) {
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
                .attribute(parseAttributes(card.getAttribute()))
                .effect(card.getCardText())
                .rarity(card.getRarity())
                .flatRarity(card.getFlatRarity())
                .cardNumber(card.getCardSetId())
                .imageUrl(card.getCardImage())
                .marketPrice(card.getMarketPrice())
                .inventoryPrice(card.getInventoryPrice())
                .errata(toErrataEntryDtoList(errataHistory))
                .faq(toFaqEntryDtoList(faqHistory))
                .build();
    }

    public List<CardDto> toDtoList(List<SetCard> cards) {
        return toDtoList(cards, Map.of(), Map.of());
    }

    public List<CardDto> toDtoList(List<SetCard> cards, Map<String, List<CardErrata>> errataByCardCode,
                                    Map<String, List<CardFaq>> faqByCardCode) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .map(card -> toDto(card, errataByCardCode.get(card.getCardSetId()), faqByCardCode.get(card.getCardSetId())))
                .collect(Collectors.toList());
    }

    public CardSummaryDto toSummaryDto(CardSummary card) {
        if (card == null) {
            return null;
        }
        return CardSummaryDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
                .cardNumber(card.getCardSetId())
                .flatRarity(card.getFlatRarity())
                .imageUrl(card.getCardImage())
                .build();
    }

    public List<CardSummaryDto> toSummaryDtoList(List<CardSummary> cards) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .map(this::toSummaryDto)
                .toList();
    }

    private List<CardErrataEntryDto> toErrataEntryDtoList(List<CardErrata> errataHistory) {
        if (errataHistory == null) {
            return List.of();
        }
        return errataHistory.stream()
                .map(e -> CardErrataEntryDto.builder()
                        .date(e.getNoticeDate() != null ? e.getNoticeDate().toString() : null)
                        .before(e.getBeforeText())
                        .after(e.getAfterText())
                        .note(e.getScopeNote())
                        .build())
                .toList();
    }

    private List<CardFaqEntryDto> toFaqEntryDtoList(List<CardFaq> faqHistory) {
        if (faqHistory == null) {
            return List.of();
        }
        return faqHistory.stream()
                .map(f -> CardFaqEntryDto.builder()
                        .question(f.getQuestion())
                        .answer(f.getAnswer())
                        .build())
                .toList();
    }

    public CardFilterOptionsDto toFilterOptionsDto(CardFilterOptions options) {
        return CardFilterOptionsDto.builder()
                .types(options.getTypes())
                .colors(options.getColors())
                .rarities(options.getRarities())
                .flatRarities(options.getFlatRarities())
                .costs(options.getCosts())
                .sets(options.getSets().stream()
                        .map(s -> CardSetOptionDto.builder().setId(s.getSetId()).setName(s.getSetName()).build())
                        .toList())
                .attributes(options.getAttributes())
                .attributeCombos(options.getAttributeCombos())
                .subTypes(options.getSubTypes())
                .prefixes(options.getPrefixes())
                .build();
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

    private List<String> parseAttributes(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return List.of();
        }
        return Arrays.stream(attribute.split("[\\s/]+"))
                .filter(token -> !token.isBlank() && !token.equalsIgnoreCase("null"))
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
