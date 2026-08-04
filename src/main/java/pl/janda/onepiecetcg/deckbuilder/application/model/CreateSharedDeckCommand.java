package pl.janda.onepiecetcg.deckbuilder.application.model;

import java.util.List;

public record CreateSharedDeckCommand(
        String name,
        String leaderCardNumber,
        List<CreateSharedDeckCardCommand> cards
) {
}
