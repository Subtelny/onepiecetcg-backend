package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCard;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCardSet;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardRepository;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardSetRepository;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OnePieceTcgApplication.class)
class CardCatalogSourceRepositoryIT extends PostgresSpringBootTest {

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;
    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;
    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;
    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;
    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;
    @MockitoBean
    private MatchupSyncUseCase matchupSyncUseCase;

    @Autowired
    private OnePieceCardSetRepository cardSetSourceRepository;

    @Autowired
    private OnePieceCardRepository cardSourceRepository;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void setUp() {
        dsl.execute("""
                CREATE TABLE IF NOT EXISTS onepiece_card_sets (
                    set_id TEXT PRIMARY KEY,
                    pack_id TEXT NOT NULL,
                    label TEXT NOT NULL,
                    card_count INTEGER NOT NULL,
                    scraped_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE TABLE IF NOT EXISTS onepiece_cards (
                    id TEXT PRIMARY KEY,
                    base_id TEXT,
                    parallel BOOLEAN NOT NULL,
                    variant_type TEXT,
                    name TEXT,
                    set_id TEXT NOT NULL REFERENCES onepiece_card_sets(set_id) ON DELETE CASCADE,
                    pack_id TEXT NOT NULL,
                    rarity TEXT,
                    finish TEXT,
                    category TEXT,
                    image_url TEXT,
                    colors TEXT,
                    cost INTEGER,
                    power INTEGER,
                    counter INTEGER,
                    attributes TEXT,
                    types TEXT,
                    source_product TEXT,
                    effect TEXT,
                    trigger TEXT,
                    scraped_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE TABLE IF NOT EXISTS cardkaizoku_card_sets (
                    set_id TEXT PRIMARY KEY,
                    source_set_id TEXT NOT NULL UNIQUE,
                    label TEXT NOT NULL,
                    release_date DATE NOT NULL,
                    source_url TEXT,
                    card_count INTEGER NOT NULL,
                    source_generated_at TIMESTAMPTZ,
                    scraped_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE TABLE IF NOT EXISTS cardkaizoku_cards (
                    id TEXT PRIMARY KEY,
                    base_id TEXT,
                    parallel BOOLEAN NOT NULL,
                    variant_type TEXT,
                    name TEXT NOT NULL,
                    set_id TEXT NOT NULL REFERENCES cardkaizoku_card_sets(set_id) ON DELETE CASCADE,
                    rarity TEXT NOT NULL,
                    finish TEXT,
                    category TEXT NOT NULL,
                    image_url TEXT NOT NULL,
                    colors TEXT NOT NULL,
                    cost INTEGER NOT NULL,
                    power INTEGER NOT NULL,
                    counter INTEGER NOT NULL,
                    attributes TEXT,
                    types TEXT NOT NULL,
                    source_product TEXT NOT NULL,
                    effect TEXT NOT NULL,
                    trigger TEXT,
                    scraped_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """);
        dsl.execute("DELETE FROM cardkaizoku_cards");
        dsl.execute("DELETE FROM cardkaizoku_card_sets");
        dsl.execute("DELETE FROM onepiece_cards");
        dsl.execute("DELETE FROM onepiece_card_sets");

        dsl.execute("""
                INSERT INTO onepiece_card_sets (set_id, pack_id, label, card_count)
                VALUES ('OP-17', 'official-op17', 'Official OP-17', 1)
                """);
        dsl.execute("""
                INSERT INTO onepiece_cards (
                    id, base_id, parallel, name, set_id, pack_id, rarity,
                    category, image_url, colors, cost, power, counter,
                    attributes, types, source_product, effect
                ) VALUES (
                    'OP17-001', NULL, FALSE, 'Official card', 'OP-17',
                    'official-op17', 'Leader', 'Leader',
                    'https://official.test/OP17-001.webp', 'Red', 5, 5000, 0,
                    'Strike', 'Official Pirates', 'Official OP-17', 'Official text'
                )
                """);

        insertLeakSet("OP-17", "OP17", "Leaked OP-17", LocalDate.now().plusDays(7));
        insertLeakCard("OP17-001", "OP-17", "Leaked duplicate");
        insertLeakSet("EB-05", "EB05", "Future EB-05", LocalDate.now().plusDays(30));
        insertLeakCard("EB05-001", "EB-05", "Future card");
        insertLeakSet("ST-99", "ST99", "Expired ST-99", LocalDate.now());
        insertLeakCard("ST99-001", "ST-99", "Expired card");
    }

    @Test
    void setCatalog_usesOfficialWholeSetPrecedenceAndKeepsOnlyFutureLeaks() {
        var sets = cardSetSourceRepository.findAll();

        assertThat(sets).extracting(OnePieceCardSet::getSetId)
                .containsExactly("EB-05", "OP-17");
        assertThat(sets).filteredOn(set -> set.getSetId().equals("OP-17"))
                .singleElement()
                .satisfies(set -> {
                    assertThat(set.getLabel()).isEqualTo("Official OP-17");
                    assertThat(set.isReleased()).isTrue();
                    assertThat(set.getReleaseDate()).isNull();
                });
        assertThat(sets).filteredOn(set -> set.getSetId().equals("EB-05"))
                .singleElement()
                .satisfies(set -> {
                    assertThat(set.isReleased()).isFalse();
                    assertThat(set.getReleaseDate()).isEqualTo(LocalDate.now().plusDays(30));
                });
    }

    @Test
    void cardCatalog_neverMixesOfficialAndLeakedRowsOfTheSameSet() {
        var cards = cardSourceRepository.findAll();

        assertThat(cards).extracting(OnePieceCard::getName)
                .containsExactly("Future card", "Official card");
        assertThat(cards).filteredOn(card -> card.getSetId().equals("OP-17"))
                .singleElement()
                .satisfies(card -> {
                    assertThat(card.getName()).isEqualTo("Official card");
                    assertThat(card.isReleased()).isTrue();
                    assertThat(card.getReleaseDate()).isNull();
                });
        assertThat(cards).filteredOn(card -> card.getSetId().equals("EB-05"))
                .singleElement()
                .satisfies(card -> {
                    assertThat(card.isReleased()).isFalse();
                    assertThat(card.getReleaseDate()).isEqualTo(LocalDate.now().plusDays(30));
                });
    }

    private void insertLeakSet(String setId, String sourceSetId, String label, LocalDate releaseDate) {
        dsl.execute("""
                INSERT INTO cardkaizoku_card_sets (
                    set_id, source_set_id, label, release_date, card_count
                ) VALUES (?, ?, ?, ?, 1)
                """, setId, sourceSetId, label, releaseDate);
    }

    private void insertLeakCard(String id, String setId, String name) {
        dsl.execute("""
                INSERT INTO cardkaizoku_cards (
                    id, base_id, parallel, name, set_id, rarity, category,
                    image_url, colors, cost, power, counter, attributes, types,
                    source_product, effect
                ) VALUES (
                    ?, NULL, FALSE, ?, ?, 'Uncommon', 'Event',
                    'https://leak.test/card.webp', 'Red', 1, 0, 0, NULL,
                    'Future Pirates', 'Future product', 'Future text'
                )
                """, id, name, setId);
    }
}
