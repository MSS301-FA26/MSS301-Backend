package com.cinemaai.gateway;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import static org.junit.jupiter.api.Assertions.*;

class GatewayRouteConfigurationTest {
    private Properties configuration() throws Exception {
        var properties = new Properties();
        try (var input = getClass().getResourceAsStream("/application.properties")) {
            properties.load(input);
        }
        return properties;
    }

    private boolean matches(Properties properties, int route, String path) {
        String value = properties.getProperty("spring.cloud.gateway.routes[" + route + "].predicates[0]");
        var config = new PathRoutePredicateFactory.Config();
        config.setPatterns(java.util.List.of(value.substring("Path=".length()).split(",")));
        return new PathRoutePredicateFactory().apply(config).test(
                MockServerWebExchange.from(MockServerHttpRequest.get(path)));
    }

    @Test void identityAliasesAndAuditLogHaveRealRoutes() throws Exception {
        var properties = configuration();
        for (String path : new String[]{"/api/v1/staff/profiles", "/api/v1/staff/profiles/42",
                "/api/v1/admin/audit-logs", "/api/v1/admin/audit-logs/"}) {
            assertTrue(matches(properties, 0, path), path);
        }
        assertFalse(matches(properties, 0, "/internal/v1/users/42"));
    }

    @Test void adaptiveSearchRoutesToAiWithoutRewritingPath() throws Exception {
        var properties = configuration();
        assertEquals("${RECOMMENDATION_SERVICE_URI}", properties.getProperty("spring.cloud.gateway.routes[13].uri"));
        assertTrue(matches(properties, 13, "/api/v1/search/adaptive"));
        assertNull(properties.getProperty("spring.cloud.gateway.routes[13].filters[0]"));
    }

    @Test void aiSecretComesOnlyFromTrustedFilter() throws Exception {
        var properties = configuration();
        assertTrue(properties.values().stream().noneMatch(value -> value.toString().contains("AddRequestHeader=X-Gateway-Secret")));
    }
}
