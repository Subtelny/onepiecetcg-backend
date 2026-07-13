package pl.janda.onepiecetcg.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.application.model.Shop;
import pl.janda.onepiecetcg.application.repository.ShopRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryShopRepository implements ShopRepository {

    private final ConcurrentHashMap<String, Shop> shops = new ConcurrentHashMap<>();

    @Override
    public List<Shop> findAll() {
        return List.copyOf(shops.values());
    }

    @Override
    public Optional<Shop> findById(String id) {
        return Optional.ofNullable(shops.get(id));
    }

    @Override
    public List<Shop> search(String name, String location) {
        return shops.values().stream()
                .filter(shop -> name == null ||
                        shop.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(shop -> location == null ||
                        shop.getLocation().toLowerCase().contains(location.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Public method for mock data loading
    public void addShop(Shop shop) {
        shops.put(shop.getId(), shop);
    }

    public void clear() {
        shops.clear();
    }
}
