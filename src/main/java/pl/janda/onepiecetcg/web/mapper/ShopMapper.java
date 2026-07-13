package pl.janda.onepiecetcg.web.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.Shop;
import pl.janda.onepiecetcg.web.dto.ShopDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShopMapper {

    public ShopDto toDto(Shop shop) {
        if (shop == null) {
            return null;
        }

        return ShopDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .location(shop.getLocation())
                .address(shop.getAddress())
                .website(shop.getWebsite())
                .phone(shop.getPhone())
                .email(shop.getEmail())
                .description(shop.getDescription())
                .openingHours(shop.getOpeningHours())
                .build();
    }

    public Shop toEntity(ShopDto dto) {
        if (dto == null) {
            return null;
        }

        return Shop.builder()
                .id(dto.getId())
                .name(dto.getName())
                .location(dto.getLocation())
                .address(dto.getAddress())
                .website(dto.getWebsite())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .description(dto.getDescription())
                .openingHours(dto.getOpeningHours())
                .build();
    }

    public List<ShopDto> toDtoList(List<Shop> shops) {
        return shops != null ?
                shops.stream().map(this::toDto).collect(Collectors.toList()) : List.of();
    }
}
