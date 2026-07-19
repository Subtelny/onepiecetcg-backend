package pl.janda.onepiecetcg.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardFilterOptionsDto {
    private List<String> types;
    private List<String> colors;
    private List<String> rarities;
    private List<String> flatRarities;
    private List<CardSetOptionDto> sets;
    private List<String> attributes;
    private List<String> subTypes;
    private List<String> prefixes;
    private List<String> effects;
}
