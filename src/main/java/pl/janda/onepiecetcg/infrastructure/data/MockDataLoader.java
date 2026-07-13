package pl.janda.onepiecetcg.infrastructure.data;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.*;
import pl.janda.onepiecetcg.infrastructure.persistence.InMemoryCardRepository;
import pl.janda.onepiecetcg.infrastructure.persistence.InMemoryDeckRepository;
import pl.janda.onepiecetcg.infrastructure.persistence.InMemoryShopRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataLoader {

    private final InMemoryCardRepository cardRepository;
    private final InMemoryDeckRepository deckRepository;
    private final InMemoryShopRepository shopRepository;

    @PostConstruct
    public void loadMockData() {
        log.info("Loading mock data...");
        loadCards();
        loadDecks();
        loadShops();
        log.info("Mock data loaded successfully!");
    }

    private void loadCards() {
        // Leaders
        Card monkeyDLuffy = createCard("card-001", "Monkey.D.Luffy", CardType.LEADER,
                List.of(CardColor.RED), null, 5000, null, null,
                "[DON!! x1] [When Attacking] Give up to 1 of your Leader or Character cards +1000 power during this battle.",
                null, CardRarity.L, "OP01-001",
                "https://onepiece-cardgame.dev/images/cards/OP01-001_p1.png");
        monkeyDLuffy.setErrata(List.of(
                CardErrata.builder()
                        .date("2023-08-04")
                        .before("[DON!! x1] [When Attacking] Give up to 1 of your Leader or Character cards +1000 power.")
                        .after("[DON!! x1] [When Attacking] Give up to 1 of your Leader or Character cards +1000 power during this battle.")
                        .note("Clarified duration of power boost")
                        .build()
        ));
        monkeyDLuffy.setFaq(List.of(
                CardFaqEntry.builder()
                        .question("Can I activate this effect multiple times in one turn?")
                        .answer("Yes, if you have enough DON!! cards attached to this Leader.")
                        .build()
        ));
        cardRepository.addCard(monkeyDLuffy);

        cardRepository.addCard(createCard("card-002", "Roronoa.Zoro", CardType.LEADER,
                List.of(CardColor.GREEN), null, 5000, null, "Slash",
                "[Your Turn] [Once Per Turn] When a DON!! card on your field is returned to your DON!! deck, give up to 1 of your Characters +1000 power during this turn.",
                null, CardRarity.L, "OP01-025", null));

        cardRepository.addCard(createCard("card-003", "Nami", CardType.LEADER,
                List.of(CardColor.BLUE), null, 5000, null, "Special",
                "[Activate: Main] You may return 1 card from your hand to your deck and shuffle your deck: Draw 2 cards.",
                null, CardRarity.L, "OP01-016", null));

        cardRepository.addCard(createCard("card-004", "Trafalgar Law", CardType.LEADER,
                List.of(CardColor.BLUE, CardColor.PURPLE), null, 5000, null, null,
                "[Activate: Main] [Once Per Turn] Choose one: Draw 1 card and trash 1 card from your hand; or return 1 of your Characters to the owner's hand.",
                null, CardRarity.L, "OP01-047", null));

        cardRepository.addCard(createCard("card-005", "Charlotte Katakuri", CardType.LEADER,
                List.of(CardColor.PURPLE), null, 5000, null, "Special",
                "[DON!! x2] [When Attacking] Look at 5 cards from the top of your deck; reveal up to 1 card with a type including Event and add it to your hand. Then, place the rest at the bottom of your deck in any order.",
                null, CardRarity.L, "OP03-099", null));

        // Characters - Red
        cardRepository.addCard(createCard("card-006", "Portgas.D.Ace", CardType.CHARACTER,
                List.of(CardColor.RED), 4, 5000, 1000, "Strike",
                "[On Play] DON!! −1 (You may return the specified number of DON!! cards from your field to your DON!! deck.): This Character gains [Rush] during this turn.",
                null, CardRarity.SR, "OP01-002", null));

        cardRepository.addCard(createCard("card-007", "Sanji", CardType.CHARACTER,
                List.of(CardColor.RED), 5, 6000, 1000, "Strike",
                "[On Play] K.O. up to 1 of your opponent's Characters with 3000 power or less.",
                null, CardRarity.R, "OP01-013", null));

        cardRepository.addCard(createCard("card-008", "Nico Robin", CardType.CHARACTER,
                List.of(CardColor.RED), 3, 4000, 1000, "Wisdom",
                "[On Play] Look at 5 cards from the top of your deck; reveal up to 1 {Straw Hat Crew} type card and add it to your hand. Then, place the rest at the bottom of your deck in any order.",
                null, CardRarity.UC, "OP01-011", null));

        // Characters - Blue
        cardRepository.addCard(createCard("card-009", "Jinbe", CardType.CHARACTER,
                List.of(CardColor.BLUE), 4, 5000, 1000, "Strike",
                "[Blocker] (After your opponent declares an attack, you may rest this card to make it the new target of the attack.)",
                null, CardRarity.R, "OP01-021", null));

        cardRepository.addCard(createCard("card-010", "Donquixote Doflamingo", CardType.CHARACTER,
                List.of(CardColor.BLUE), 7, 7000, null, "Special",
                "[On Play] Return up to 2 Characters with a cost of 3 or less to the owner's hand.",
                null, CardRarity.SR, "OP01-018", null));

        // Characters - Green
        cardRepository.addCard(createCard("card-011", "Roronoa.Zoro", CardType.CHARACTER,
                List.of(CardColor.GREEN), 4, 5000, 2000, "Slash",
                "[DON!! x1] [When Attacking] Give this Character +1000 power during this battle.",
                null, CardRarity.SR, "OP01-025", null));

        cardRepository.addCard(createCard("card-012", "Usopp", CardType.CHARACTER,
                List.of(CardColor.GREEN), 2, 3000, 1000, "Ranged",
                "[Blocker]",
                null, CardRarity.C, "OP01-032", null));

        cardRepository.addCard(createCard("card-013", "Tony Tony.Chopper", CardType.CHARACTER,
                List.of(CardColor.GREEN), 1, 2000, 1000, "Wisdom",
                "[On Play] If your Leader's type includes {Straw Hat Crew}, this Character gains [Rush] during this turn.",
                null, CardRarity.UC, "OP01-035", null));

        // Characters - Purple
        cardRepository.addCard(createCard("card-014", "Gecko Moria", CardType.CHARACTER,
                List.of(CardColor.PURPLE), 5, 6000, null, "Special",
                "[On Play] Trash 2 cards from the top of your opponent's Life cards.",
                null, CardRarity.SR, "OP02-093", null));

        cardRepository.addCard(createCard("card-015", "Perona", CardType.CHARACTER,
                List.of(CardColor.PURPLE), 2, 2000, 1000, "Special",
                "[On Play] Look at 3 cards from the top of your deck and place them at the top or bottom of the deck in any order.",
                null, CardRarity.C, "OP02-099", null));

        // Characters - Yellow
        cardRepository.addCard(createCard("card-016", "Borsalino", CardType.CHARACTER,
                List.of(CardColor.YELLOW), 7, 7000, null, "Ranged",
                "[On Play] Return up to 1 Character with a cost of 5 or less to the owner's hand.",
                null, CardRarity.SR, "OP02-114", null));

        cardRepository.addCard(createCard("card-017", "Kizaru", CardType.CHARACTER,
                List.of(CardColor.YELLOW), 8, 8000, null, "Ranged",
                "[On Play] K.O. up to 1 of your opponent's Characters with 6000 power or less.",
                null, CardRarity.SEC, "OP02-125", null));

        // Characters - Black
        cardRepository.addCard(createCard("card-018", "Marshall.D.Teach", CardType.CHARACTER,
                List.of(CardColor.BLACK), 6, 7000, null, "Special",
                "[On Play] Draw 2 cards and trash 2 cards from your hand.",
                null, CardRarity.SR, "OP03-108", null));

        cardRepository.addCard(createCard("card-019", "Shiryu", CardType.CHARACTER,
                List.of(CardColor.BLACK), 4, 5000, 1000, "Slash",
                "[On K.O.] Play up to 1 {Blackbeard Pirates} type Character card with a cost of 4 or less from your hand.",
                null, CardRarity.R, "OP03-113", null));

        // Events
        cardRepository.addCard(createCard("card-020", "Gum-Gum Red Roc", CardType.EVENT,
                List.of(CardColor.RED), 6, null, null, null,
                "[Main] K.O. up to 1 of your opponent's Characters with 7000 power or less.",
                null, CardRarity.R, "OP01-015", null));

        cardRepository.addCard(createCard("card-021", "Radical Beam", CardType.EVENT,
                List.of(CardColor.BLUE), 2, null, null, null,
                "[Counter] Up to 1 of your Leader or Character cards gains +4000 power during this battle.",
                "+1000", CardRarity.UC, "OP01-023", null));

        cardRepository.addCard(createCard("card-022", "Three Thousand Worlds", CardType.EVENT,
                List.of(CardColor.GREEN), 1, null, null, null,
                "[Main] Up to 1 of your Leader or Character cards gains +2000 power during this turn.",
                null, CardRarity.C, "OP01-044", null));

        cardRepository.addCard(createCard("card-023", "Moria's Guidance", CardType.EVENT,
                List.of(CardColor.PURPLE), 3, null, null, null,
                "[Main] Look at 5 cards from the top of your deck and place them at the top or bottom of the deck in any order.",
                null, CardRarity.UC, "OP02-100", null));

        // Stages
        cardRepository.addCard(createCard("card-024", "Thousand Sunny", CardType.STAGE,
                List.of(CardColor.RED), 1, null, null, null,
                "[Activate: Main] You may rest this Stage: If your Leader's type includes {Straw Hat Crew}, give up to 1 of your Leader or Character cards +1000 power during this turn.",
                null, CardRarity.R, "OP01-007", null));

        cardRepository.addCard(createCard("card-025", "Fish-Man Island", CardType.STAGE,
                List.of(CardColor.BLUE), 2, null, null, null,
                "[Activate: Main] You may rest this Stage: Look at 3 cards from the top of your deck and place them at the top or bottom of the deck in any order.",
                null, CardRarity.UC, "OP01-022", null));

        // Multi-color characters
        cardRepository.addCard(createCard("card-026", "Boa Hancock", CardType.CHARACTER,
                List.of(CardColor.BLUE, CardColor.GREEN), 4, 5000, 1000, "Special",
                "[On Play] Return up to 1 Character with a cost of 3 or less to the owner's hand.",
                null, CardRarity.R, "OP01-078", null));

        cardRepository.addCard(createCard("card-027", "Shanks", CardType.CHARACTER,
                List.of(CardColor.RED, CardColor.GREEN), 9, 10000, null, "Slash",
                "[On Play] K.O. up to 1 of your opponent's Characters with 8000 power or less.",
                null, CardRarity.SEC, "OP01-120", null));

        cardRepository.addCard(createCard("card-028", "Kaido", CardType.CHARACTER,
                List.of(CardColor.PURPLE, CardColor.GREEN), 10, 12000, null, "Strike",
                "[On Play] K.O. up to 1 of your opponent's Characters with 10000 power or less.",
                null, CardRarity.SEC, "OP02-122", null));

        // Promo cards
        cardRepository.addCard(createCard("card-029", "Monkey.D.Luffy (Promo)", CardType.CHARACTER,
                List.of(CardColor.RED), 5, 6000, 1000, "Strike",
                "[Rush] [DON!! x1] [When Attacking] If you have 3 or less Life cards, this Character gains +2000 power during this battle.",
                null, CardRarity.PR, "P-001", null));

        cardRepository.addCard(createCard("card-030", "Yamato", CardType.CHARACTER,
                List.of(CardColor.GREEN, CardColor.YELLOW), 5, 5000, 2000, "Strike",
                "[On Play] DON!! −1: Play up to 1 Character card with a cost of 4 or less from your hand.",
                null, CardRarity.SR, "OP03-041", null));

        log.info("Loaded {} cards", 30);
    }

    private Card createCard(String id, String name, CardType type, List<CardColor> color,
                           Integer cost, Integer power, Integer counter, String attribute,
                           String effect, String trigger, CardRarity rarity, String cardNumber,
                           String imageUrl) {
        return Card.builder()
                .id(id)
                .name(name)
                .type(type)
                .color(color)
                .cost(cost)
                .power(power)
                .counter(counter)
                .attribute(attribute)
                .effect(effect)
                .trigger(trigger)
                .rarity(rarity)
                .cardNumber(cardNumber)
                .imageUrl(imageUrl)
                .build();
    }

    private void loadDecks() {
        // Get some cards for deck building
        Card luffyLeader = cardRepository.findById("card-001").orElseThrow();
        Card ace = cardRepository.findById("card-006").orElseThrow();
        Card sanji = cardRepository.findById("card-007").orElseThrow();
        Card robin = cardRepository.findById("card-008").orElseThrow();
        Card gumGumRedRoc = cardRepository.findById("card-020").orElseThrow();
        Card thousandSunny = cardRepository.findById("card-024").orElseThrow();

        Deck redRushDeck = createDeck(
                "deck-001",
                "Red Rush Aggro",
                "Aggressive red deck focused on early board control and rushing the opponent.",
                luffyLeader,
                Arrays.asList(
                        new DeckCard(ace, 4),
                        new DeckCard(sanji, 4),
                        new DeckCard(robin, 4),
                        new DeckCard(gumGumRedRoc, 3),
                        new DeckCard(thousandSunny, 2)
                ),
                "LuffyFan123"
        );
        deckRepository.addDeck(redRushDeck);

        // Deck 2 - Green Zoro
        Card zoroLeader = cardRepository.findById("card-002").orElseThrow();
        Card zoroChar = cardRepository.findById("card-011").orElseThrow();
        Card usopp = cardRepository.findById("card-012").orElseThrow();
        Card chopper = cardRepository.findById("card-013").orElseThrow();

        Deck greenZoroDeck = createDeck(
                "deck-002",
                "Green Zoro Control",
                "Control-oriented green deck with Zoro as leader.",
                zoroLeader,
                Arrays.asList(
                        new DeckCard(zoroChar, 4),
                        new DeckCard(usopp, 4),
                        new DeckCard(chopper, 4)
                ),
                "SwordMaster"
        );
        deckRepository.addDeck(greenZoroDeck);

        // Deck 3 - Blue Nami
        Card namiLeader = cardRepository.findById("card-003").orElseThrow();
        Card jinbe = cardRepository.findById("card-009").orElseThrow();
        Card doflamingo = cardRepository.findById("card-010").orElseThrow();

        Deck blueNamiDeck = createDeck(
                "deck-003",
                "Blue Nami Bounce",
                "Bounce strategy with Nami, returning opponent's characters.",
                namiLeader,
                Arrays.asList(
                        new DeckCard(jinbe, 4),
                        new DeckCard(doflamingo, 2)
                ),
                "Navigator99"
        );
        deckRepository.addDeck(blueNamiDeck);

        // Deck 4 - Purple Katakuri
        Card katakuriLeader = cardRepository.findById("card-005").orElseThrow();
        Card moria = cardRepository.findById("card-014").orElseThrow();
        Card perona = cardRepository.findById("card-015").orElseThrow();

        Deck purpleKatakuriDeck = createDeck(
                "deck-004",
                "Purple Katakuri Events",
                "Event-focused purple deck with Katakuri.",
                katakuriLeader,
                Arrays.asList(
                        new DeckCard(moria, 3),
                        new DeckCard(perona, 4)
                ),
                "MochiKing"
        );
        deckRepository.addDeck(purpleKatakuriDeck);

        // Deck 5 - Law Blue/Purple
        Card lawLeader = cardRepository.findById("card-004").orElseThrow();

        Deck lawDeck = createDeck(
                "deck-005",
                "Law Dual Color",
                "Blue/Purple deck utilizing Law's dual color advantage.",
                lawLeader,
                Arrays.asList(
                        new DeckCard(jinbe, 3),
                        new DeckCard(moria, 2),
                        new DeckCard(perona, 3)
                ),
                "SurgeonOfDeath"
        );
        deckRepository.addDeck(lawDeck);

        // Deck 6-10 (simplified versions)
        for (int i = 6; i <= 10; i++) {
            Deck deck = createDeck(
                    "deck-00" + i,
                    "Sample Deck " + i,
                    "This is a sample deck for testing purposes.",
                    luffyLeader,
                    Arrays.asList(
                            new DeckCard(ace, 4),
                            new DeckCard(sanji, 3)
                    ),
                    "TestUser" + i
            );
            deckRepository.addDeck(deck);
        }

        log.info("Loaded {} decks", 10);
    }

    private Deck createDeck(String id, String name, String description, Card leader,
                           List<DeckCard> cards, String author) {
        return Deck.builder()
                .id(id)
                .name(name)
                .description(description)
                .leader(leader)
                .cards(cards != null ? new ArrayList<>(cards) : new ArrayList<>())
                .createdAt(LocalDateTime.now().minusDays((long) (Math.random() * 30)))
                .updatedAt(LocalDateTime.now())
                .author(author)
                .build();
    }

    private void loadShops() {
        shopRepository.addShop(createShop("shop-001", "Kamerat Games", "Warszawa",
                "ul. Marszałkowska 115, 00-102 Warszawa", "https://kameratgames.pl",
                "+48 22 123 4567", "kontakt@kameratgames.pl",
                "Sklep z kartami kolekcjonerskimi i grami planszowymi w centrum Warszawy.",
                "Pn-Pt: 12:00-20:00, Sb: 10:00-18:00, Nd: nieczynne"));

        shopRepository.addShop(createShop("shop-002", "Magia i Miecz", "Kraków",
                "ul. Floriańska 34, 31-021 Kraków", "https://magiaimiecz.pl",
                "+48 12 234 5678", "sklep@magiaimiecz.pl",
                "Krakowski sklep specjalizujący się w TCG i RPG.",
                "Pn-Sb: 11:00-19:00, Nd: 12:00-17:00"));

        shopRepository.addShop(createShop("shop-003", "Rebel Games", "Gdańsk",
                "ul. Długa 45, 80-831 Gdańsk", "https://rebelgames.pl",
                "+48 58 345 6789", "info@rebelgames.pl",
                "Sklep z kartami One Piece TCG i innymi grami kolekcjonerskimi.",
                "Pn-Pt: 10:00-19:00, Sb-Nd: 10:00-16:00"));

        shopRepository.addShop(createShop("shop-004", "BlackLotus", "Wrocław",
                "ul. Świdnicka 23, 50-066 Wrocław", "https://blacklotus.pl",
                "+48 71 456 7890", "sklep@blacklotus.pl",
                "Profesjonalny sklep TCG z bogatym asortymentem One Piece.",
                "Pn-Pt: 11:00-20:00, Sb: 10:00-18:00, Nd: nieczynne"));

        shopRepository.addShop(createShop("shop-005", "GameZone", "Poznań",
                "ul. Półwiejska 42, 61-888 Poznań", "https://gamezone-poznan.pl",
                "+48 61 567 8901", "kontakt@gamezone.pl",
                "Sklep i miejsce spotkań dla fanów TCG w Poznaniu.",
                "Pn-Sb: 12:00-20:00, Nd: 12:00-18:00"));

        shopRepository.addShop(createShop("shop-006", "Karciana Przystań", "Łódź",
                "ul. Piotrkowska 96, 90-006 Łódź", "https://karcianaprzymstan.pl",
                "+48 42 678 9012", "info@karcianaprzymstan.pl",
                "Łódzki sklep z kartami One Piece TCG i organizowane turnieje.",
                "Pn-Pt: 13:00-21:00, Sb-Nd: 11:00-19:00"));

        shopRepository.addShop(createShop("shop-007", "Dragon's Lair", "Katowice",
                "ul. Mariacka 12, 40-014 Katowice", "https://dragonslair.pl",
                "+48 32 789 0123", "sklep@dragonslair.pl",
                "Śląski sklep TCG z friendly atmosferą i regularnymi eventami.",
                "Pn-Pt: 12:00-20:00, Sb: 10:00-18:00, Nd: nieczynne"));

        shopRepository.addShop(createShop("shop-008", "Strefa Gier", "Szczecin",
                "ul. Bogurodzicy 8, 70-440 Szczecin", "https://strefagier.pl",
                "+48 91 890 1234", "kontakt@strefagier.pl",
                "Sklep z kartami i grami w Szczecinie. Specjalizacja One Piece TCG.",
                "Pn-Sb: 11:00-19:00, Nd: 12:00-17:00"));

        shopRepository.addShop(createShop("shop-009", "Nerd Kingdom", "Lublin",
                "ul. Krakowskie Przedmieście 62, 20-002 Lublin", "https://nerdkingdom.pl",
                "+48 81 901 2345", "info@nerdkingdom.pl",
                "Królestwo nerdów w Lublinie - TCG, RPG, gry planszowe.",
                "Pn-Pt: 10:00-20:00, Sb: 10:00-18:00, Nd: nieczynne"));

        shopRepository.addShop(createShop("shop-010", "Cardverse", "Bydgoszcz",
                "ul. Gdańska 141, 85-021 Bydgoszcz", "https://cardverse.pl",
                "+48 52 012 3456", "sklep@cardverse.pl",
                "Uniwersum kart w Bydgoszczy. Najlepsze ceny na One Piece TCG!",
                "Pn-Pt: 12:00-20:00, Sb-Nd: 11:00-18:00"));

        log.info("Loaded {} shops", 10);
    }

    private Shop createShop(String id, String name, String location, String address,
                           String website, String phone, String email, String description,
                           String openingHours) {
        return Shop.builder()
                .id(id)
                .name(name)
                .location(location)
                .address(address)
                .website(website)
                .phone(phone)
                .email(email)
                .description(description)
                .openingHours(openingHours)
                .build();
    }
}
