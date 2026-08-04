package pl.janda.onepiecetcg.deckbuilder.application.port.in;

import pl.janda.onepiecetcg.deckbuilder.application.model.CreateSharedDeckCommand;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeckDetails;

public interface SharedDeckUseCase {

    SharedDeckDetails createSharedDeck(CreateSharedDeckCommand command);

    SharedDeckDetails getSharedDeck(String shareCode);
}
