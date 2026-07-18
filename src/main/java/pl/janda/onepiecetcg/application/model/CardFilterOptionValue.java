package pl.janda.onepiecetcg.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_filter_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardFilterOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private CardFilterOptionCategory category;

    @Column(name = "value", nullable = false)
    private String value;

    // Only populated for category == SET, holding the set's display name (setId is stored in `value`).
    @Column(name = "label")
    private String label;
}
