package pl.janda.onepiecetcg.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shop {
    private String id;
    private String name;
    private String location;
    private String address;
    private String website;
    private String phone;
    private String email;
    private String description;
    private String openingHours;
}
