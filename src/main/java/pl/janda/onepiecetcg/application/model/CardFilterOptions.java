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
public class CardFilterOptions {

    private List<String> types;

    private List<String> colors;

    private List<String> rarities;

    private List<String> flatRarities;

    private List<CardSet> sets;

    private List<String> attributes;

    private List<String> attributeCombos;

    private List<String> subTypes;

    private List<String> prefixes;

    private List<String> effects;
}
