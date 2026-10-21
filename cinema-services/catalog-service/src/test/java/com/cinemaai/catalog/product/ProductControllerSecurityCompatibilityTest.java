package com.cinemaai.catalog.product;

import com.cinemaai.catalog.controller.AdminProductController;
import com.cinemaai.catalog.controller.InternalProductController;
import com.cinemaai.catalog.controller.ProductController;
import com.cinemaai.catalog.controller.StaffProductController;
import com.cinemaai.catalog.repository.ProductBundleRepository;
import com.cinemaai.catalog.repository.ProductRepository;
import com.cinemaai.catalog.security.SecurityConfig;
import com.cinemaai.catalog.service.ProductCategoryService;
import com.cinemaai.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ProductController.class,
        AdminProductController.class,
        StaffProductController.class,
        InternalProductController.class
})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=product-controller-test",
        "LOG_LEVEL_PATTERN=%5p",
        "server.address=127.0.0.1",
        "server.port=0",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class ProductControllerSecurityCompatibilityTest {

    @Autowired MockMvc mvc;
    @MockBean ProductService productService;
    @MockBean ProductCategoryService productCategoryService;
    @MockBean ProductRepository productRepository;
    @MockBean ProductBundleRepository productBundleRepository;

    @Test
    void publicProductListingKeepsLegacyRoute() throws Exception {
        mvc.perform(get("/api/v1/foods/items")
                        .header("X-Gateway-Secret", "gateway-test"))
                .andExpect(status().isOk());
    }

    @Test
    void adminMutationAllowsAdminAndRejectsCustomer() throws Exception {
        String body = """
                {"name":"Water","description":"Bottle","price":20000,"status":"ACTIVE","categoryId":3}
                """;
        mvc.perform(post("/api/v1/admin/foods/items")
                        .headers(roleHeaders("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/foods/items")
                        .headers(roleHeaders("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void staffMutationKeepsLegacyRouteAndRoleBoundary() throws Exception {
        mvc.perform(patch("/api/v1/staff/foods/items/11/status")
                        .headers(roleHeaders("CUSTOMER"))
                        .param("status", "LOW_STOCK"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/staff/foods/items/11/status")
                        .headers(roleHeaders("STAFF"))
                        .param("status", "LOW_STOCK"))
                .andExpect(status().isOk());
    }

    @Test
    void internalQuoteRequiresInternalSecretRatherThanGatewaySecret() throws Exception {
        String body = "{\"foods\":[{\"productId\":11,\"isCombo\":false,\"quantity\":1}]}";
        mvc.perform(post("/internal/v1/foods/quote")
                        .header("X-Gateway-Secret", "gateway-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/v1/foods/quote")
                        .header("X-Internal-Service-Secret", "internal-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void internalQuoteAcceptsCanonicalFieldAndRejectsDualFieldsAtHttpBoundary() throws Exception {
        mvc.perform(post("/internal/v1/foods/quote")
                        .header("X-Internal-Service-Secret", "internal-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"products\":[{\"productId\":11,\"isCombo\":false,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/internal/v1/foods/quote")
                        .header("X-Internal-Service-Secret", "internal-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foods\":[],\"products\":[]}"))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.http.HttpHeaders roleHeaders(String role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Gateway-Secret", "gateway-test");
        headers.set("X-User-Id", "7");
        headers.set("X-User-Roles", role);
        return headers;
    }
}
