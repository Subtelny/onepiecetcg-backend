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

    @Parameter(description = "Card type (LEADER, CHARACTER, EVENT, STAGE)")
    private CardType type;

    @Parameter(description = "Card colors (RED, BLUE, GREEN, PURPLE, YELLOW, BLACK)")
    private List<String> color;

    @Parameter(description = "Card rarities (C, UC, R, SR, L, PR, SEC, TR)")
    private List<String> rarity;

    @Parameter(description = "Exact card cost")
    private Integer cost;

    @Parameter(description = "Exact card power")
    private Integer power;

    @Parameter(description = "Set ID the card belongs to, e.g. OP01")
    private String setId;

    @Parameter(description = "Exact counter amount")
    private Integer counterAmount;

    @Parameter(description = "Card attribute, e.g. Slash, Strike")
    private String attribute;

    @Parameter(description = "Sub-type to search for (e.g. Straw Hat Crew)")
    private String subTypes;
}
