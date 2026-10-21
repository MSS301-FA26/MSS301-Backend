package com.cinemaai.catalog.product;

import com.cinemaai.catalog.controller.AdminProductController;
import com.cinemaai.catalog.controller.InternalProductController;
import com.cinemaai.catalog.controller.ProductController;
import com.cinemaai.catalog.controller.StaffProductController;
import com.cinemaai.catalog.dto.request.product.ProductQuoteRequest;
import com.cinemaai.catalog.dto.response.product.ProductQuoteResponse;
import com.cinemaai.catalog.entity.Product;
import com.cinemaai.catalog.entity.ProductBundle;
import com.cinemaai.catalog.entity.ProductCategory;
import com.cinemaai.catalog.enums.ProductStatus;
import com.cinemaai.catalog.repository.ProductBundleRepository;
import com.cinemaai.catalog.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductDomainCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void legacyQuoteJsonKeepsFoodAndProductIdentityFields() throws Exception {
        ProductQuoteRequest request = objectMapper.readValue("""
                {"foods":[
                  {"foodItemId":11,"foodComboId":null,"productId":11,"isCombo":false,"quantity":2},
                  {"foodItemId":null,"foodComboId":11,"productId":11,"isCombo":true,"quantity":1}
                ]}
                """, ProductQuoteRequest.class);

        assertEquals(2, request.foods().size());
        assertEquals(11L, request.foods().get(0).productId());
        assertFalse(request.foods().get(0).isCombo());
        assertEquals(11L, request.foods().get(1).productId());
        assertTrue(request.foods().get(1).isCombo());
    }

    @Test
    void productAndBundleWithSameNumericIdRemainDistinct() throws Exception {
        ProductRepository products = mock(ProductRepository.class);
        ProductBundleRepository bundles = mock(ProductBundleRepository.class);
        Product product = withId(new Product("Water", "Bottle", new BigDecimal("20000")), 11L);
        ProductBundle bundle = withId(new ProductBundle("Combo", "Bundle", new BigDecimal("70000")), 11L);
        when(products.findById(11L)).thenReturn(Optional.of(product));
        when(bundles.findById(11L)).thenReturn(Optional.of(bundle));
        InternalProductController controller = new InternalProductController(products, bundles);

        ProductQuoteRequest request = new ProductQuoteRequest(java.util.List.of(
                new ProductQuoteRequest.Item(null, null, 11L, false, 2),
                new ProductQuoteRequest.Item(null, null, 11L, true, 1)));
        ProductQuoteResponse quote = controller.quoteFoods(request).data();

        assertEquals(2, quote.items().size());
        assertFalse(quote.items().get(0).isCombo());
        assertTrue(quote.items().get(1).isCombo());
        assertEquals(new BigDecimal("110000"), quote.totalAmount());
    }

    @Test
    void renamedEntitiesKeepLegacyTablesAndStringStatusPersistence() throws Exception {
        assertEquals("food_items", Product.class.getAnnotation(Table.class).name());
        assertEquals("food_combos", ProductBundle.class.getAnnotation(Table.class).name());
        assertEquals("food_categories", ProductCategory.class.getAnnotation(Table.class).name());
        assertEquals(EnumType.STRING,
                Product.class.getDeclaredField("status").getAnnotation(Enumerated.class).value());
        assertEquals(EnumType.STRING,
                ProductBundle.class.getDeclaredField("status").getAnnotation(Enumerated.class).value());
        assertEquals(ProductStatus.LOW_STOCK, ProductStatus.valueOf("LOW_STOCK"));
    }

    @Test
    void renamedControllersKeepUniqueLegacyBasePaths() {
        assertEquals("/api/v1/foods", basePath(ProductController.class));
        assertEquals("/api/v1/admin/foods", basePath(AdminProductController.class));
        assertEquals("/api/v1/staff/foods", basePath(StaffProductController.class));
        assertEquals("/internal/v1/foods", basePath(InternalProductController.class));
    }

    private String basePath(Class<?> controller) {
        return controller.getAnnotation(RequestMapping.class).value()[0];
    }

    private <T> T withId(T entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
        return entity;
    }
}
