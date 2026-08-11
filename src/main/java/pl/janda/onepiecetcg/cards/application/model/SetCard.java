package pl.janda.onepiecetcg.cards.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "set_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetCard {

    public static final String DEFAULT_VARIANT_INDEX = "0";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_set_id")
    private String cardSetId;

    @Column(name = "card_prefix")
    private String cardPrefix;

    @Column(name = "card_name")
    private String cardName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "source_product")
    private String sourceProduct;

    @Column(name = "set_id")
    private String setId;

    @Column(name = "set_name")
    private String setName;

    @Column(name = "card_text", columnDefinition = "TEXT")
    private String cardText;

    @Column(name = "rarity")
    private String rarity;

    @Column(name = "flat_rarity")
    private String flatRarity;

    @Column(name = "card_color")
    private String cardColor;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "life")
    private String life;

    @Column(name = "card_cost")
    private String cardCost;

    @Column(name = "card_power")
    private String cardPower;

    @Column(name = "sub_types")
    private String subTypes;

    @Column(name = "counter_amount")
    private Integer counterAmount;

    @Column(name = "attribute")
    private String attribute;

    @Column(name = "card_image_id")
    private String cardImageId;

    @Column(name = "card_image")
    private String cardImage;

    @Column(name = "inventory_price")
    private Double inventoryPrice;

    @Column(name = "market_price")
    private Double marketPrice;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Builder.Default
    @Column(name = "variant_index", nullable = false, length = 16, columnDefinition = "varchar(16) default '0'")
    private String variantIndex = DEFAULT_VARIANT_INDEX;

    public boolean isRepresentative() {
        return DEFAULT_VARIANT_INDEX.equals(variantIndex);
    }
}
