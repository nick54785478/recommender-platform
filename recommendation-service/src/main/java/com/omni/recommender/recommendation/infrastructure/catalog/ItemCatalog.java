package com.omni.recommender.recommendation.infrastructure.catalog;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ItemCatalog {

    @Data
    public static class ItineraryItem {
        private final String id;
        private final List<String> tags;

        public ItineraryItem(String id, List<String> tags) {
            this.id = id;
            this.tags = tags;
        }
    }

    private final Map<String, ItineraryItem> database = Map.of(
            "TPE-NRT-PROMO", new ItineraryItem("TPE-NRT-PROMO", List.of("City", "Food", "Culture")),
            "KHH-KIX-HOT", new ItineraryItem("KHH-KIX-HOT", List.of("Theme Park", "Heritage", "Family")),
            "TPE-BKK-SALE", new ItineraryItem("TPE-BKK-SALE", List.of("Relaxation", "Nightlife", "Budget")),
            "ICN-SEOUL-WINTER", new ItineraryItem("ICN-SEOUL-WINTER", List.of("Winter", "Shopping", "K-Culture")),
            "SIN-MARINA-LUX", new ItineraryItem("SIN-MARINA-LUX", List.of("Luxury", "City", "Sightseeing")),
            "HKG-DIMSUM-WKND", new ItineraryItem("HKG-DIMSUM-WKND", List.of("Food", "Weekend", "Budget"))
    );

    public List<ItineraryItem> getAllItems() {
        return database.values().stream().collect(Collectors.toList());
    }
    
    public ItineraryItem getItem(String id) {
        return database.get(id);
    }
}
