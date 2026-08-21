package pl.janda.onepiecetcg.matchups.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchupLeaderId implements Serializable {

    private String dataset;

    private String cardCode;
}
