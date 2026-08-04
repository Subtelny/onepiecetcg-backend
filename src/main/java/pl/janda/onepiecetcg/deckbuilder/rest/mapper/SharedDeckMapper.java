package pl.janda.onepiecetcg.deckbuilder.rest.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.deckbuilder.application.model.CreateSharedDeckCardCommand;
import pl.janda.onepiecetcg.deckbuilder.application.model.CreateSharedDeckCommand;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeckDetails;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.CreateSharedDeckRequest;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.SharedDeckCardDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.SharedDeckCreatedDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.SharedDeckDto;

@Component
@RequiredArgsConstructor
public class SharedDeckMapper {

    private static final String SHARED_DECK_PATH_PREFIX = "/d/";

    private final DeckBuilderCardMapper deckBuilderCardMapper;

    public CreateSharedDeckCommand toCommand(CreateSharedDeckRequest request) {
        return new CreateSharedDeckCommand(
                request.getName(),
                request.getLeaderCardNumber(),
                request.getCards().stream()
                        .map(card -> new CreateSharedDeckCardCommand(card.getCardNumber(), card.getQuantity()))
                        .toList());
    }

    public SharedDeckCreatedDto toCreatedDto(SharedDeckDetails details) {
        var code = details.deck().getShareCode();
        return SharedDeckCreatedDto.builder()
                .code(code)
                .path(SHARED_DECK_PATH_PREFIX + code)
                .build();
    }

    public SharedDeckDto toDto(SharedDeckDetails details) {
        var deck = details.deck();
        return SharedDeckDto.builder()
                .code(deck.getShareCode())
                .name(deck.getName())
                .leader(deckBuilderCardMapper.toDto(details.leader()))
                .cards(details.cards().stream()
                        .map(card -> SharedDeckCardDto.builder()
                                .card(deckBuilderCardMapper.toDto(card.card()))
                                .quantity(card.entry().getQuantity())
                                .build())
                        .toList())
                .createdAt(deck.getCreatedAt().toString())
                .build();
    }
}
