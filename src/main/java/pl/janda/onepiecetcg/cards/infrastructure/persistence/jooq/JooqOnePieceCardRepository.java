package pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCard;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqOnePieceCardRepository implements OnePieceCardRepository {

    private final DSLContext dsl;

    @Override
    public List<OnePieceCard> findAll() {
        return dsl.fetch("""
                        WITH catalog_cards AS (
                            SELECT c.id,
                                   c.base_id,
                                   c.name,
                                   c.set_id,
                                   s.label AS set_name,
                                   c.rarity,
                                   c.category,
                                   c.image_url,
                                   c.colors,
                                   c.cost,
                                   c.power,
                                   c.counter,
                                   c.attributes,
                                   c.types,
                                   c.source_product,
                                   c.effect,
                                   c.trigger,
                                   TRUE AS released,
                                   NULL::date AS release_date,
                                   c.scraped_at
                            FROM onepiece_cards c
                            JOIN onepiece_card_sets s ON s.set_id = c.set_id
                        
                            UNION ALL
                        
                            SELECT c.id,
                                   c.base_id,
                                   c.name,
                                   c.set_id,
                                   s.label AS set_name,
                                   c.rarity,
                                   c.category,
                                   c.image_url,
                                   c.colors,
                                   c.cost,
                                   c.power,
                                   c.counter,
                                   c.attributes,
                                   c.types,
                                   c.source_product,
                                   c.effect,
                                   c.trigger,
                                   FALSE AS released,
                                   s.release_date,
                                   c.scraped_at
                            FROM cardkaizoku_cards c
                            JOIN cardkaizoku_card_sets s ON s.set_id = c.set_id
                            WHERE s.release_date > CURRENT_DATE
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM onepiece_card_sets official
                                  WHERE official.set_id = s.set_id
                              )
                        )
                        SELECT *
                        FROM catalog_cards
                        ORDER BY COALESCE(base_id, id), base_id IS NOT NULL, set_id, id
                        """)
                .map(record -> OnePieceCard.builder()
                        .id(record.get("id", String.class))
                        .baseId(record.get("base_id", String.class))
                        .name(record.get("name", String.class))
                        .setId(record.get("set_id", String.class))
                        .setName(record.get("set_name", String.class))
                        .rarity(record.get("rarity", String.class))
                        .category(record.get("category", String.class))
                        .imageUrl(record.get("image_url", String.class))
                        .colors(record.get("colors", String.class))
                        .cost(record.get("cost", Integer.class))
                        .power(record.get("power", Integer.class))
                        .counter(record.get("counter", Integer.class))
                        .attributes(record.get("attributes", String.class))
                        .types(record.get("types", String.class))
                        .sourceProduct(record.get("source_product", String.class))
                        .effect(record.get("effect", String.class))
                        .trigger(record.get("trigger", String.class))
                        .released(Boolean.TRUE.equals(record.get("released", Boolean.class)))
                        .releaseDate(record.get("release_date", LocalDate.class))
                        .scrapedAt(record.get("scraped_at", OffsetDateTime.class))
                        .build());
    }
}
