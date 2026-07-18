package pl.janda.onepiecetcg.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_set_id")
    private String cardSetId;

    @Column(name = "card_prefix")
    private String cardPrefix;

    @Column(name = "card_name")
    private String cardName;

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

    @Column(name = "date_scraped")
    private String dateScraped;

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

    @Column(name = "is_promo", nullable = false)
    private boolean promo;

    @Column(name = "is_representative", nullable = false, columnDefinition = "boolean default false")
    private boolean representative;
}
