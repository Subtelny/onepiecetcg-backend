package pl.janda.onepiecetcg.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deck {
    private String id;
    private String name;
    private String description;
    private Card leader;
    private List<DeckCard> cards;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String author;
}
