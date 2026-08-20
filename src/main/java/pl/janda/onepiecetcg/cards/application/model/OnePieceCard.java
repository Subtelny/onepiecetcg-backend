package pl.janda.onepiecetcg.cards.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnePieceCard {

    private String id;

    private String baseId;

    private String name;

    private String setId;

    private String setName;

    private String rarity;

    private String category;

    private String imageUrl;

    private String colors;

    private Integer cost;

    private Integer power;

    private Integer counter;

    private String attributes;

    private String types;

    private String sourceProduct;

    private String effect;

    private String trigger;

    @Builder.Default
    private boolean released = true;

    private LocalDate releaseDate;

    private OffsetDateTime scrapedAt;
}
