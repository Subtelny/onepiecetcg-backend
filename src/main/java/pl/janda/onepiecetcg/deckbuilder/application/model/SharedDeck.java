package pl.janda.onepiecetcg.deckbuilder.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shared_decks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedDeck {

    @Id
    @Column(name = "share_code", nullable = false, length = 10, updatable = false)
    private String shareCode;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "leader_card_number", length = 32)
    private String leaderCardNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "shared_deck_cards",
            joinColumns = @JoinColumn(name = "share_code"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_shared_deck_card_number",
                    columnNames = {"share_code", "card_number"}))
    @Builder.Default
    private List<SharedDeckCard> cards = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
