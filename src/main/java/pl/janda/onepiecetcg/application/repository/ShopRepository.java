package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.Shop;

import java.util.List;
import java.util.Optional;

public interface ShopRepository {

    List<Shop> findAll();

    Optional<Shop> findById(String id);

    List<Shop> search(String name, String location);
}
