package pl.janda.onepiecetcg.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private String id;
    private String name;
    private CardType type;
    private List<CardColor> color;
    private Integer cost;
    private Integer power;
    private Integer counter;
    private String attribute;
    private String effect;
    private String trigger;
    private CardRarity rarity;
    private String cardNumber;
    private String imageUrl;

    // Embedded errata and FAQ (user confirmed)
    private List<CardErrata> errata;
    private List<CardFaqEntry> faq;
}
