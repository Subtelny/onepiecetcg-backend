package pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.model.PriceableCard;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JooqPriceableCardQueryAdapter {

    private final DSLContext dsl;

    public List<PriceableCard> findAll() {
        return dsl.fetch("""
                        SELECT price_reference,
                               card_id,
                               card_set_id,
                               set_id,
                               source_product,
                               set_name,
                               variant_index
                        FROM set_cards
                        WHERE price_reference IS NOT NULL
                        ORDER BY card_set_id, set_id, variant_index, card_id
                        """)
                .map(record -> PriceableCard.builder()
                        .priceReference(record.get("price_reference", String.class))
                        .sourceCardId(record.get("card_id", String.class))
                        .cardCode(record.get("card_set_id", String.class))
                        .releaseId(record.get("set_id", String.class))
                        .releaseName(record.get("source_product", String.class))
                        .setName(record.get("set_name", String.class))
                        .variantIndex(record.get("variant_index", String.class))
                        .build());
    }
}
