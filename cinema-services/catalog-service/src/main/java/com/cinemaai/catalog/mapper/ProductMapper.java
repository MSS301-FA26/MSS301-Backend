package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.product.ProductBundleResponse;
import com.cinemaai.catalog.dto.response.product.ProductResponse;
import com.cinemaai.catalog.entity.ProductCategory;
import com.cinemaai.catalog.entity.ProductBundle;
import com.cinemaai.catalog.entity.Product;
import com.cinemaai.catalog.repository.ProductCategoryRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ProductMapper {

    @Autowired
    protected ProductCategoryRepository productCategoryRepository;

    @Mapping(target = "categoryName", source = "categoryId", qualifiedByName = "lookupCategoryName")
    public abstract ProductResponse toFoodItemResponse(Product foodItem);

    public abstract ProductBundleResponse toFoodComboResponse(ProductBundle foodCombo);

    @org.mapstruct.Named("lookupCategoryName")
    protected String lookupCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return productCategoryRepository.findById(categoryId)
                .map(ProductCategory::getName)
                .orElse(null);
    }
}
