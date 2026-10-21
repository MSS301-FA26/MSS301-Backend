package com.cinemaai.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinemaai.catalog.dto.request.product.ProductBundleRequest;
import com.cinemaai.catalog.dto.request.product.ProductCategoryRequest;
import com.cinemaai.catalog.dto.request.product.ProductRequest;
import com.cinemaai.catalog.entity.Product;
import com.cinemaai.catalog.entity.ProductBundle;
import com.cinemaai.catalog.entity.ProductCategory;
import com.cinemaai.catalog.enums.ProductStatus;
import com.cinemaai.catalog.mapper.ProductMapper;
import com.cinemaai.catalog.repository.ProductBundleRepository;
import com.cinemaai.catalog.repository.ProductCategoryRepository;
import com.cinemaai.catalog.repository.ProductRepository;
import com.cinemaai.catalog.service.AuditLogService;
import com.cinemaai.catalog.service.impl.ProductCategoryServiceImpl;
import com.cinemaai.catalog.service.impl.ProductServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceCompatibilityTest {

    @Mock ProductRepository productRepository;
    @Mock ProductBundleRepository productBundleRepository;
    @Mock ProductMapper productMapper;
    @Mock AuditLogService auditLogService;
    @InjectMocks ProductServiceImpl productService;

    @Mock ProductCategoryRepository productCategoryRepository;
    @InjectMocks ProductCategoryServiceImpl productCategoryService;

    @Test
    void createItemKeepsLegacyProductBehavior() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new ProductRequest("Popcorn", "Large", new BigDecimal("75000"),
                "image", ProductStatus.LOW_STOCK, 12L);

        productService.createItem(request);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Popcorn");
        assertThat(captor.getValue().getCategoryId()).isEqualTo(12L);
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.LOW_STOCK);
    }

    @Test
    void createBundleKeepsLegacyComboBehavior() {
        when(productBundleRepository.save(any(ProductBundle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new ProductBundleRequest("Couple Combo", "Bundle", new BigDecimal("125000"),
                "image", ProductStatus.ACTIVE);

        productService.createCombo(request);

        var captor = ArgumentCaptor.forClass(ProductBundle.class);
        verify(productBundleRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Couple Combo");
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("125000");
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void createCategoryKeepsLegacyCategoryBehavior() {
        when(productCategoryRepository.existsByCode("DRINK")).thenReturn(false);
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new ProductCategoryRequest(" drink ", "Drink", "Cold drinks", 3, null);

        var response = productCategoryService.createCategory(request);

        assertThat(response.code()).isEqualTo("DRINK");
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(productCategoryRepository).save(any(ProductCategory.class));
    }
}
