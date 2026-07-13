package pl.janda.onepiecetcg.web.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.Deck;
import pl.janda.onepiecetcg.application.model.DeckCard;
import pl.janda.onepiecetcg.web.dto.CreateDeckRequest;
import pl.janda.onepiecetcg.web.dto.DeckCardDto;
import pl.janda.onepiecetcg.web.dto.DeckDto;
import pl.janda.onepiecetcg.web.dto.UpdateDeckRequest;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DeckMapper {

    private final CardMapper cardMapper;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public DeckDto toDto(Deck deck) {
        if (deck == null) {
            return null;
        }

        return DeckDto.builder()
                .id(deck.getId())
                .name(deck.getName())
                .description(deck.getDescription())
                .leader(cardMapper.toDto(deck.getLeader()))
                .cards(deck.getCards() != null ?
                        deck.getCards().stream()
                                .map(this::deckCardToDto)
                                .collect(Collectors.toList()) : null)
                .createdAt(deck.getCreatedAt() != null ?
                        deck.getCreatedAt().format(ISO_FORMATTER) : null)
                .updatedAt(deck.getUpdatedAt() != null ?
                        deck.getUpdatedAt().format(ISO_FORMATTER) : null)
                .author(deck.getAuthor())
                .build();
    }

    public Deck toEntity(CreateDeckRequest request) {
        if (request == null) {
            return null;
        }

        return Deck.builder()
                .name(request.getName())
                .description(request.getDescription())
                .leader(cardMapper.toEntity(request.getLeader()))
                .cards(request.getCards() != null ?
                        request.getCards().stream()
                                .map(this::deckCardToEntity)
                                .collect(Collectors.toList()) : null)
                .author(request.getAuthor())
                .build();
    }

    public Deck toEntity(String id, UpdateDeckRequest request, Deck existing) {
        if (request == null) {
            return existing;
        }

        return Deck.builder()
                .id(id)
                .name(request.getName() != null ? request.getName() : existing.getName())
                .description(request.getDescription() != null ?
                        request.getDescription() : existing.getDescription())
                .leader(request.getLeader() != null ?
                        cardMapper.toEntity(request.getLeader()) : existing.getLeader())
                .cards(request.getCards() != null ?
                        request.getCards().stream()
                                .map(this::deckCardToEntity)
                                .collect(Collectors.toList()) : existing.getCards())
                .createdAt(existing.getCreatedAt())
                .author(request.getAuthor() != null ? request.getAuthor() : existing.getAuthor())
                .build();
    }

    private DeckCardDto deckCardToDto(DeckCard deckCard) {
        return DeckCardDto.builder()
                .card(cardMapper.toDto(deckCard.getCard()))
                .quantity(deckCard.getQuantity())
                .build();
    }

    private DeckCard deckCardToEntity(DeckCardDto dto) {
        return DeckCard.builder()
                .card(cardMapper.toEntity(dto.getCard()))
                .quantity(dto.getQuantity())
                .build();
    }

    public List<DeckDto> toDtoList(List<Deck> decks) {
        return decks != null ?
                decks.stream().map(this::toDto).collect(Collectors.toList()) : List.of();
    }
}
