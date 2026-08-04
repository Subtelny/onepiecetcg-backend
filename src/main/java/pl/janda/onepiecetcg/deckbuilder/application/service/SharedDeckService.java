package pl.janda.onepiecetcg.deckbuilder.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.deckbuilder.application.model.*;
import pl.janda.onepiecetcg.deckbuilder.application.port.in.SharedDeckUseCase;
import pl.janda.onepiecetcg.deckbuilder.application.repository.SharedDeckRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedDeckService implements SharedDeckUseCase {

    private static final String SHARE_CODE_ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private static final int SHARE_CODE_LENGTH = 10;

    private static final int SHARE_CODE_GENERATION_ATTEMPTS = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SharedDeckRepository sharedDeckRepository;

    private final CardCatalogUseCase cardCatalogUseCase;

    @Override
    public SharedDeckDetails createSharedDeck(CreateSharedDeckCommand command) {
        validate(command);
        var cards = command.cards().stream()
                .map(card -> SharedDeckCard.builder()
                        .cardNumber(card.cardNumber().trim())
                        .quantity(card.quantity())
                        .build())
                .toList();
        var leaderCardNumber = blankToNull(command.leaderCardNumber());
        var sharedDeck = SharedDeck.builder()
                .shareCode(generateShareCode())
                .name(command.name().trim())
                .leaderCardNumber(leaderCardNumber)
                .cards(cards)
                .createdAt(Instant.now())
                .build();

        var details = resolveDetails(sharedDeck);
        var saved = sharedDeckRepository.save(sharedDeck);
        return new SharedDeckDetails(saved, details.leader(), details.cards());
    }

    @Override
    public SharedDeckDetails getSharedDeck(String shareCode) {
        var sharedDeck = sharedDeckRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new IllegalArgumentException("Shared deck not found with code: " + shareCode));
        return resolveDetails(sharedDeck);
    }

    private SharedDeckDetails resolveDetails(SharedDeck sharedDeck) {
        var cardNumbers = new LinkedHashSet<String>();
        if (sharedDeck.getLeaderCardNumber() != null) {
            cardNumbers.add(sharedDeck.getLeaderCardNumber());
        }
        sharedDeck.getCards().stream()
                .map(SharedDeckCard::getCardNumber)
                .forEach(cardNumbers::add);

        var cardsByNumber = cardCatalogUseCase.getRepresentativeCardsByCardCodes(List.copyOf(cardNumbers)).stream()
                .collect(Collectors.toMap(SetCard::getCardSetId, Function.identity()));

        var leader = resolveLeader(sharedDeck.getLeaderCardNumber(), cardsByNumber);
        var cards = sharedDeck.getCards().stream()
                .map(entry -> new SharedDeckCardDetails(entry, resolveDeckCard(entry.getCardNumber(), cardsByNumber)))
                .toList();
        return new SharedDeckDetails(sharedDeck, leader, cards);
    }

    private void validate(CreateSharedDeckCommand command) {
        if (command == null || command.name() == null || command.name().isBlank()
                || command.name().trim().length() > 80) {
            throw new IllegalArgumentException("Shared deck name must contain between 1 and 80 characters");
        }
        if (command.leaderCardNumber() != null && command.leaderCardNumber().trim().length() > 32) {
            throw new IllegalArgumentException("Leader card number cannot exceed 32 characters");
        }
        if (command.cards() == null || command.cards().size() > 50) {
            throw new IllegalArgumentException("A shared deck can contain at most 50 distinct deck cards");
        }
        if (blankToNull(command.leaderCardNumber()) == null && command.cards().isEmpty()) {
            throw new IllegalArgumentException("A shared deck must contain a leader or at least one deck card");
        }

        var cardNumbers = new HashSet<String>();
        var totalQuantity = 0;
        for (var card : command.cards()) {
            if (card == null || card.cardNumber() == null || card.cardNumber().isBlank()
                    || card.cardNumber().trim().length() > 32) {
                throw new IllegalArgumentException("Every deck card must have a card number of at most 32 characters");
            }
            if (card.quantity() < 1 || card.quantity() > 4) {
                throw new IllegalArgumentException("Every deck card quantity must be between 1 and 4");
            }
            if (!cardNumbers.add(card.cardNumber().trim())) {
                throw new IllegalArgumentException("Deck card numbers must be unique");
            }
            totalQuantity += card.quantity();
        }
        if (totalQuantity > 50) {
            throw new IllegalArgumentException("The total deck card quantity cannot exceed 50");
        }
    }

    private SetCard resolveLeader(String cardNumber, Map<String, SetCard> cardsByNumber) {
        if (cardNumber == null) {
            return null;
        }
        var card = requireCard(cardNumber, cardsByNumber);
        if (!"LEADER".equalsIgnoreCase(card.getCardType())) {
            throw new IllegalArgumentException("Card is not a leader: " + cardNumber);
        }
        return card;
    }

    private SetCard resolveDeckCard(String cardNumber, Map<String, SetCard> cardsByNumber) {
        var card = requireCard(cardNumber, cardsByNumber);
        if ("LEADER".equalsIgnoreCase(card.getCardType())) {
            throw new IllegalArgumentException("Leader cannot be included among deck cards: " + cardNumber);
        }
        return card;
    }

    private SetCard requireCard(String cardNumber, Map<String, SetCard> cardsByNumber) {
        var card = cardsByNumber.get(cardNumber);
        if (card == null) {
            throw new IllegalArgumentException("Card not found with number: " + cardNumber);
        }
        return card;
    }

    private String generateShareCode() {
        for (var attempt = 0; attempt < SHARE_CODE_GENERATION_ATTEMPTS; attempt++) {
            var code = randomShareCode();
            if (!sharedDeckRepository.existsByShareCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique shared deck code");
    }

    private String randomShareCode() {
        var code = new StringBuilder(SHARE_CODE_LENGTH);
        for (var i = 0; i < SHARE_CODE_LENGTH; i++) {
            code.append(SHARE_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(SHARE_CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
