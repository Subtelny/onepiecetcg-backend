package pl.janda.onepiecetcg.cards.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnePieceCardSet {

    private String setId;

    private String label;

    @Builder.Default
    private boolean released = true;

    private LocalDate releaseDate;
}
