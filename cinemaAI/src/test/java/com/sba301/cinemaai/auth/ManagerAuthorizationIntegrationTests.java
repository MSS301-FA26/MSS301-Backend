package com.sba301.cinemaai.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ManagerAuthorizationIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRoleRepository userRoleRepository;
    @Autowired CinemaRepository cinemaRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void managerIsAssignedOnlyRequestedCinemaAndCannotCallAdminApis() throws Exception {
        String adminToken = createAndLoginAdmin();
        Cinema first = cinemaRepository.save(new Cinema("Manager Test A " + System.nanoTime(), "A", "City", null));
        Cinema second = cinemaRepository.save(new Cinema("Manager Test B " + System.nanoTime(), "B", "City", null));
        String email = "manager." + System.nanoTime() + "@example.com";
        String body = "{\"email\":\"" + email + "\",\"password\":\"Manager123!\",\"fullName\":\"Manager Test\",\"cinemaIds\":[" + first.getId() + "]}";
        mockMvc.perform(post("/api/v1/admin/users/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roles[0]").value("MANAGER"));

        String managerToken = login(email, "Manager123!");
        mockMvc.perform(get("/api/v1/manager/cinemas").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(first.getId()));
        mockMvc.perform(get("/api/v1/manager/cinemas/{id}/inventory", second.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/reports/revenue").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/movies/1/approve").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        long managerId = userRepository.findByEmail(email).orElseThrow().getId();

        // 1. Scoped reports
        mockMvc.perform(get("/api/v1/manager/cinemas/{id}/reports/overview", first.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cinemaId").value(first.getId()));

        mockMvc.perform(get("/api/v1/manager/cinemas/{id}/reports/overview", second.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // 2. Scoped staff management
        String staffEmail = "cinema.staff." + System.nanoTime() + "@example.com";
        String staffBody = "{\"email\":\"" + staffEmail + "\",\"password\":\"Staff123!\",\"fullName\":\"Cinema Staff\",\"phone\":\"0987654321\"}";
        String createStaffRes = mockMvc.perform(post("/api/v1/manager/cinemas/{id}/staff", first.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(staffBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(staffEmail))
                .andReturn().getResponse().getContentAsString();
        long staffId = objectMapper.readTree(createStaffRes).at("/data/id").asLong();

        // Staff list
        mockMvc.perform(get("/api/v1/manager/cinemas/{id}/staff", first.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value(staffEmail));

        // Cannot create staff in unassigned cinema
        mockMvc.perform(post("/api/v1/manager/cinemas/{id}/staff", second.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(staffBody))
                .andExpect(status().isForbidden());

        // Toggle staff status
        mockMvc.perform(patch("/api/v1/manager/cinemas/{cinemaId}/staff/{staffId}/status", first.getId(), staffId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        // 3. Scoped audit logs
        mockMvc.perform(get("/api/v1/manager/cinemas/{id}/audit-logs", first.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/manager/cinemas/{id}/audit-logs", second.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // 4. Admin approval endpoint
        mockMvc.perform(get("/api/v1/admin/movie-edit-requests")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/movie-edit-requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 5. Manager deactivation blocks access immediately
        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", managerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/manager/cinemas").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }


    private String createAndLoginAdmin() throws Exception {
        String email = "manager.admin." + System.nanoTime() + "@example.com";
        User admin = new User(email, passwordEncoder.encode("Admin123!"), "Test Admin", null);
        admin.setEmailVerified(true);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));
        userRoleRepository.save(new UserRole(admin, role));
        return login(email, "Admin123!");
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.at("/data/accessToken").asText();
    }
}
