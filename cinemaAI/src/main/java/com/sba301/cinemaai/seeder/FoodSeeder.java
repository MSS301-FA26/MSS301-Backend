package com.sba301.cinemaai.seeder;

import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.repository.FoodComboRepository;
import com.sba301.cinemaai.repository.FoodItemRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(45)
@RequiredArgsConstructor
public class FoodSeeder implements Seeder {

    private final FoodItemRepository foodItemRepository;
    private final FoodComboRepository foodComboRepository;

    @Override
    @Transactional
    public void seed() {
        seedItem(new FoodSeed(
                "Bắp rang bơ",
                "Bắp rang nóng phủ bơ thơm, size vừa cho một người.",
                "45000",
                "https://images.unsplash.com/photo-1585647347483-22b66260dfff?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.ACTIVE
        ));
        seedItem(new FoodSeed(
                "Bắp phô mai",
                "Bắp rang phủ bột phô mai đậm vị.",
                "55000",
                "https://images.unsplash.com/photo-1578849278619-e73505e9610f?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.LOW_STOCK
        ));
        seedItem(new FoodSeed(
                "Coca-Cola",
                "Ly Coca-Cola lạnh size L.",
                "30000",
                "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.ACTIVE
        ));
        seedItem(new FoodSeed(
                "Pepsi",
                "Ly Pepsi lạnh size L.",
                "30000",
                "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.ACTIVE
        ));
        seedItem(new FoodSeed(
                "Nước suối",
                "Chai nước suối 500ml.",
                "20000",
                "https://images.unsplash.com/photo-1616118132534-381148898bb4?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.ACTIVE
        ));
        seedItem(new FoodSeed(
                "Trà đào",
                "Trà đào lạnh thơm nhẹ, size L.",
                "35000",
                "https://images.unsplash.com/photo-1556679343-c7306c1976bc?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.OUT_OF_STOCK
        ));

        seedCombo(new FoodSeed(
                "Combo Solo",
                "1 bắp rang bơ + 1 nước ngọt size L.",
                "69000",
                "https://images.unsplash.com/photo-1619881589316-56c7f9e6b587?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.ACTIVE
        ));
        seedCombo(new FoodSeed(
                "Combo Couple",
                "1 bắp rang bơ lớn + 2 nước ngọt size L.",
                "119000",
                "https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.ACTIVE
        ));
        seedCombo(new FoodSeed(
                "Combo Family",
                "2 bắp rang bơ + 4 nước ngọt size L.",
                "229000",
                "https://images.unsplash.com/photo-1599490659213-e2b9527bd087?q=80&w=600&auto=format&fit=crop",
                FoodItemStatus.LOW_STOCK
        ));
    }

    private void seedItem(FoodSeed seed) {
        FoodItem item = foodItemRepository.findByNameIgnoreCase(seed.name())
                .orElseGet(() -> foodItemRepository.save(new FoodItem(seed.name(), seed.description(), seed.price())));
        item.setName(seed.name());
        item.setDescription(seed.description());
        item.setPrice(seed.price());
        item.setImageUrl(seed.imageUrl());
        item.setStatus(seed.status());
    }

    private void seedCombo(FoodSeed seed) {
        FoodCombo combo = foodComboRepository.findByNameIgnoreCase(seed.name())
                .orElseGet(() -> foodComboRepository.save(new FoodCombo(seed.name(), seed.description(), seed.price())));
        combo.setName(seed.name());
        combo.setDescription(seed.description());
        combo.setPrice(seed.price());
        combo.setImageUrl(seed.imageUrl());
        combo.setStatus(seed.status());
    }

    private record FoodSeed(
            String name,
            String description,
            String priceText,
            String imageUrl,
            FoodItemStatus status
    ) {

        private BigDecimal price() {
            return new BigDecimal(priceText);
        }
    }
}
