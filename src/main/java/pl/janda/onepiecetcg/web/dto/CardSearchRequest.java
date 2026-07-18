package pl.janda.onepiecetcg.web.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.janda.onepiecetcg.application.model.CardType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSearchRequest {

    @Parameter(description = "Card name or card number to search")
    private String name;

    @Parameter(description = "Card types (LEADER, CHARACTER, EVENT, STAGE)")
    private List<CardType> types;

    @Parameter(description = "Card colors (RED, BLUE, GREEN, PURPLE, YELLOW, BLACK)")
    private List<String> color;

    @Parameter(description = "Card rarities (C, UC, R, SR, L, PR, SEC, TR)")
    private List<String> rarity;

    @Parameter(description = "Exact card cost")
    private Integer cost;

    @Parameter(description = "Exact card power")
    private Integer power;

    @Parameter(description = "Exact counter amount")
    private Integer counterAmount;

    @Parameter(description = "Card attributes, e.g. Slash, Strike")
    private List<String> attributes;

    @Parameter(description = "Sub-type to search for (e.g. Straw Hat Crew)")
    private String subTypes;

    @Parameter(description = "Card prefixes, e.g. ST, OP01, EB01")
    private List<String> prefixes;

    @Parameter(description = "Page number, 0-indexed. Defaults to 0.")
    private Integer page;

    @Parameter(description = "Page size. Defaults to 50.")
    private Integer limit;
}
