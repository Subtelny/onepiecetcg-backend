package pl.janda.onepiecetcg.cards.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OptcgSetResponse(
        @JsonProperty("set_name") String setName,
        @JsonProperty("set_id") String setId
) {
}
