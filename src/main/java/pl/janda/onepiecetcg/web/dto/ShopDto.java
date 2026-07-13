package pl.janda.onepiecetcg.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopDto {
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
