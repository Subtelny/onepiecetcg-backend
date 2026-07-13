package pl.janda.onepiecetcg.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.janda.onepiecetcg.application.model.Shop;
import pl.janda.onepiecetcg.application.service.ShopService;
import pl.janda.onepiecetcg.web.dto.ShopDto;
import pl.janda.onepiecetcg.web.mapper.ShopMapper;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Tag(name = "Shops", description = "Shop directory endpoints")
public class ShopController {

    private final ShopService shopService;
    private final ShopMapper shopMapper;

    @GetMapping
    @Operation(summary = "Get all shops or search with filters",
               description = "Returns all shops or filtered shops based on query parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shops retrieved successfully",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ShopDto.class)))
    })
    public ResponseEntity<List<ShopDto>> searchShops(
            @Parameter(description = "Shop name to search")
            @RequestParam(required = false) String name,

            @Parameter(description = "Shop location to search")
            @RequestParam(required = false) String location
    ) {
        List<Shop> shops = shopService.searchShops(name, location);
        return ResponseEntity.ok(shopMapper.toDtoList(shops));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shop by ID", description = "Returns a single shop by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shop found",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ShopDto.class))),
        @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<ShopDto> getShopById(
            @Parameter(description = "Shop ID") @PathVariable String id
    ) {
        Shop shop = shopService.getShopById(id);
        return ResponseEntity.ok(shopMapper.toDto(shop));
    }
}
