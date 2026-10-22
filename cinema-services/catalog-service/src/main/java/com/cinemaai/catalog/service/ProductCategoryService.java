package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.product.ProductCategoryRequest;
import com.cinemaai.catalog.dto.response.product.ProductCategoryResponse;

import java.util.List;

public interface ProductCategoryService {
    List<ProductCategoryResponse> getActiveCategories();
    List<ProductCategoryResponse> getAllCategories();
    ProductCategoryResponse getCategoryById(Long id);
    ProductCategoryResponse createCategory(ProductCategoryRequest request);
    ProductCategoryResponse updateCategory(Long id, ProductCategoryRequest request);
    ProductCategoryResponse deleteCategory(Long id);
    ProductCategoryResponse restoreCategory(Long id);
}
