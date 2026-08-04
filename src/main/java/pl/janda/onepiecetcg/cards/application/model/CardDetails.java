package pl.janda.onepiecetcg.cards.application.model;

import java.util.List;


public record CardDetails(SetCard card, List<CardErrata> errata, List<CardFaq> faq) {
}
