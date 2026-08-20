package pl.janda.onepiecetcg.cards.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_sets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSet {

    @Id
    @Column(name = "set_id")
    private String setId;

    @Column(name = "set_name", nullable = false)
    private String setName;

    @Builder.Default
    @Column(name = "released", nullable = false, columnDefinition = "boolean default true")
    private boolean released = true;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}
