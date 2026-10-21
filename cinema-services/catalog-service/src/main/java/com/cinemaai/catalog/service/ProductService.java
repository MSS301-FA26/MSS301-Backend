package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.product.ProductBundleRequest;
import com.cinemaai.catalog.dto.request.product.ProductRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.product.ProductBundleResponse;
import com.cinemaai.catalog.dto.response.product.ProductResponse;
import com.cinemaai.catalog.entity.ProductBundle;
import com.cinemaai.catalog.entity.Product;
import com.cinemaai.catalog.enums.ProductStatus;
import java.util.List;

public interface ProductService {

    List<ProductResponse> getActiveItems();

    PageResponse<ProductResponse> getActiveItems(int page, int size);

    List<ProductBundleResponse> getActiveCombos();

    PageResponse<ProductBundleResponse> getActiveCombos(int page, int size);

    List<ProductResponse> getAllItems();

    PageResponse<ProductResponse> getAllItems(int page, int size);

    List<ProductBundleResponse> getAllCombos();

    PageResponse<ProductBundleResponse> getAllCombos(int page, int size);

    ProductResponse createItem(ProductRequest request);

    ProductBundleResponse createCombo(ProductBundleRequest request);

    ProductResponse updateItem(Long id, ProductRequest request);

    ProductBundleResponse updateCombo(Long id, ProductBundleRequest request);

    ProductResponse updateItemStatus(Long id, ProductStatus status);

    ProductBundleResponse updateComboStatus(Long id, ProductStatus status);

    ProductResponse deleteItem(Long id);

    ProductBundleResponse deleteCombo(Long id);

    Product findItem(Long id);

    ProductBundle findCombo(Long id);
}
