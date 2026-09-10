package com.sba301.cinemaai.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthEndpointInventoryTests {

    @Test
    void authControllerShouldExposePhase2Endpoints() throws IOException {
        String controller = readSource("controller/AuthController.java");

        List<String> endpoints = List.of(
                "@postmapping(\"/register\")",
                "@postmapping(\"/login\")",
                "@postmapping(\"/google\")",
                "@postmapping(\"/google/verify\")",
                "@postmapping(\"/refresh\")",
                "@postmapping(\"/logout\")",
                "@postmapping(\"/verify-email\")",
                "@postmapping(\"/verify-email/request\")",
                "@postmapping(\"/password-reset/request\")",
                "@postmapping(\"/password-reset/confirm\")"
        );

        for (String endpoint : endpoints) {
            assertTrue(controller.contains(endpoint), "AuthController must expose " + endpoint);
        }
    }

    @Test
    void userControllerShouldExposeSelfServiceEndpoints() throws IOException {
        String controller = readSource("controller/UserController.java");

        List<String> endpoints = List.of(
                "@getmapping(\"/me\")",
                "@putmapping(\"/me\")",
                "@postmapping(\"/me/avatar\")",
                "@postmapping(\"/me/password\")"
        );

        for (String endpoint : endpoints) {
            assertTrue(controller.contains(endpoint), "UserController must expose " + endpoint);
        }
    }

    @Test
    void adminUserControllerShouldExposeAdminManagementEndpoints() throws IOException {
        String controller = readSource("controller/AdminUserController.java");

        List<String> endpoints = List.of(
                "@requestmapping(\"/api/v1/admin/users\")",
                "@getmapping",
                "@getmapping(\"/{userid}\")",
                "@postmapping(\"/staff\")",
                "@patchmapping(\"/{userid}/status\")"
        );

        for (String endpoint : endpoints) {
            assertTrue(controller.contains(endpoint), "AdminUserController must expose " + endpoint);
        }
    }

    @Test
    void phase2RequestDtosShouldContainSecurityCriticalFields() throws IOException {
        String registerRequest = readSource("dto/request/auth/RegisterRequest.java");
        assertTrue(registerRequest.contains("string email"));
        assertTrue(registerRequest.contains("string password"));
        assertTrue(registerRequest.contains("string fullname"));
        assertTrue(registerRequest.contains("integer birthyear"));

        String authResponse = readSource("dto/response/auth/AuthResponse.java");
        assertTrue(authResponse.contains("string accesstoken"));
        assertTrue(authResponse.contains("string refreshtoken"));
        assertTrue(authResponse.contains("long expiresinms"));
        assertTrue(authResponse.contains("userprofileresponse user"));
        assertTrue(authResponse.contains("list<string> roles"));
    }

    private String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/sba301/cinemaai").resolve(relativePath)).toLowerCase();
    }
}
