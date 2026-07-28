package pl.janda.onepiecetcg.cards.application.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class FlatRarityOverrideProperties {

    private static final String RESOURCE_NAME = "flat-rarity-overrides.properties";

    private final Map<String, String> overrides;

    public FlatRarityOverrideProperties() {
        this.overrides = load();
    }

    public Optional<String> get(String cardSetId) {
        return Optional.ofNullable(overrides.get(cardSetId));
    }

    private static Map<String, String> load() {
        var properties = new Properties();
        try (var input = FlatRarityOverrideProperties.class.getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + RESOURCE_NAME, e);
        }
        return properties.stringPropertyNames().stream()
                .collect(Collectors.toMap(name -> name, properties::getProperty));
    }
}
