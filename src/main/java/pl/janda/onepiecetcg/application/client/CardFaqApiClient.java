package pl.janda.onepiecetcg.application.client;

import pl.janda.onepiecetcg.application.model.CardFaq;
import pl.janda.onepiecetcg.application.model.CardFaqListingEntry;

import java.time.LocalDate;
import java.util.List;

public interface CardFaqApiClient {

    List<CardFaqListingEntry> fetchFaqListing();

    List<CardFaq> fetchFaqEntries(String setId, LocalDate publishedDate, String pdfUrl);
}
