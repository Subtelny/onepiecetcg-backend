package pl.janda.onepiecetcg.deckbuilder.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedDeckCard {

    @Column(name = "card_number", nullable = false, length = 32)
    private String cardNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
