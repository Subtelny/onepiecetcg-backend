package pl.janda.onepiecetcg.web.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.Card;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardErrata;
import pl.janda.onepiecetcg.application.model.CardFaqEntry;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.web.dto.CardDto;
import pl.janda.onepiecetcg.web.dto.CardErrataDto;
import pl.janda.onepiecetcg.web.dto.CardFaqEntryDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CardMapper {

    public CardDto toDto(Card card) {
        if (card == null) {
            return null;
        }

        return CardDto.builder()
                .id(card.getId())
                .name(card.getName())
                .type(card.getType() != null ? card.getType().name() : null)
                .color(card.getColor() != null ?
                        card.getColor().stream()
                                .map(CardColor::name)
                                .collect(Collectors.toList()) : null)
                .cost(card.getCost())
                .power(card.getPower())
                .counter(card.getCounter())
                .attribute(card.getAttribute())
                .effect(card.getEffect())
                .trigger(card.getTrigger())
                .rarity(card.getRarity() != null ? card.getRarity().name() : null)
                .cardNumber(card.getCardNumber())
                .imageUrl(card.getImageUrl())
                .errata(card.getErrata() != null ?
                        card.getErrata().stream()
                                .map(this::errataToDto)
                                .collect(Collectors.toList()) : null)
                .faq(card.getFaq() != null ?
                        card.getFaq().stream()
                                .map(this::faqToDto)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    public Card toEntity(CardDto dto) {
        if (dto == null) {
            return null;
        }

        return Card.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType() != null ? CardType.valueOf(dto.getType()) : null)
                .color(dto.getColor() != null ?
                        dto.getColor().stream()
                                .map(CardColor::valueOf)
                                .collect(Collectors.toList()) : null)
                .cost(dto.getCost())
                .power(dto.getPower())
                .counter(dto.getCounter())
                .attribute(dto.getAttribute())
                .effect(dto.getEffect())
                .trigger(dto.getTrigger())
                .rarity(dto.getRarity() != null ? CardRarity.valueOf(dto.getRarity()) : null)
                .cardNumber(dto.getCardNumber())
                .imageUrl(dto.getImageUrl())
                .errata(dto.getErrata() != null ?
                        dto.getErrata().stream()
                                .map(this::errataToEntity)
                                .collect(Collectors.toList()) : null)
                .faq(dto.getFaq() != null ?
                        dto.getFaq().stream()
                                .map(this::faqToEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    private CardErrataDto errataToDto(CardErrata errata) {
        return CardErrataDto.builder()
                .date(errata.getDate())
                .before(errata.getBefore())
                .after(errata.getAfter())
                .note(errata.getNote())
                .build();
    }

    private CardErrata errataToEntity(CardErrataDto dto) {
        return CardErrata.builder()
                .date(dto.getDate())
                .before(dto.getBefore())
                .after(dto.getAfter())
                .note(dto.getNote())
                .build();
    }

    private CardFaqEntryDto faqToDto(CardFaqEntry faq) {
        return CardFaqEntryDto.builder()
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .build();
    }

    private CardFaqEntry faqToEntity(CardFaqEntryDto dto) {
        return CardFaqEntry.builder()
                .question(dto.getQuestion())
                .answer(dto.getAnswer())
                .build();
    }

    public List<CardDto> toDtoList(List<Card> cards) {
        return cards != null ?
                cards.stream().map(this::toDto).collect(Collectors.toList()) : List.of();
    }
}
