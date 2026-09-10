package com.sba301.cinemaai.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.dto.request.ticket.TicketComboRequest;
import com.sba301.cinemaai.dto.request.ticket.TicketPricingRuleRequest;
import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminTicketPricingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldManageRulesWithPagingAndRejectDuplicateActiveRule() throws Exception {
        String token = loginAsAdmin();
        TicketPricingRuleRequest request = new TicketPricingRuleRequest(
                TicketType.ADULT,
                RoomType.IMAX,
                SeatType.STANDARD,
                true,
                true,
                BigDecimal.valueOf(90000),
                true
        );

        String createdRuleResponse = mockMvc.perform(post("/api/v1/admin/ticket-pricing/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.price").value(90000))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long ruleId = objectMapper.readTree(createdRuleResponse).at("/data/id").asLong();

        mockMvc.perform(get("/api/v1/admin/ticket-pricing/rules")
                        .header("Authorization", "Bearer " + token)
                        .param("ticketType", "ADULT")
                        .param("roomType", "IMAX")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(ruleId));

        mockMvc.perform(post("/api/v1/admin/ticket-pricing/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Quy tắc giá vé đang hoạt động đã tồn tại cho loại vé, loại phòng, loại ghế, cuối tuần và ngày lễ này"));

        mockMvc.perform(put("/api/v1/admin/ticket-pricing/rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketPricingRuleRequest(
                                TicketType.ADULT,
                                RoomType.IMAX,
                                SeatType.STANDARD,
                                true,
                                true,
                                BigDecimal.valueOf(95000),
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value(95000));

        mockMvc.perform(delete("/api/v1/admin/ticket-pricing/rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldValidateRuleAndComboPriceRange() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/v1/admin/ticket-pricing/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketPricingRuleRequest(
                                TicketType.STUDENT,
                                RoomType.VIP,
                                SeatType.STANDARD,
                                false,
                                false,
                                BigDecimal.valueOf(9999),
                                true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"))
                .andExpect(jsonPath("$.errors[0].message").value("Giá phải từ 10.000đ trở lên"));

        mockMvc.perform(post("/api/v1/admin/ticket-pricing/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketPricingRuleRequest(
                                TicketType.CHILD,
                                RoomType.VIP,
                                SeatType.COUPLE,
                                false,
                                false,
                                BigDecimal.valueOf(120000),
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ticketType").value("CHILD"))
                .andExpect(jsonPath("$.data.seatType").value("COUPLE"));

        mockMvc.perform(post("/api/v1/admin/ticket-pricing/combos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketComboRequest(
                                "High Price Combo " + System.nanoTime(),
                                "Validation test",
                                1,
                                0,
                                0,
                                BigDecimal.valueOf(1000001),
                                true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"))
                .andExpect(jsonPath("$.errors[0].message").value("Giá không được lớn hơn 1.000.000đ"));
    }

    @Test
    void shouldReturnInactiveComboConflictMessageAndSupportAdminComboPaging() throws Exception {
        String token = loginAsAdmin();
        String comboName = "Inactive Combo " + System.nanoTime();
        TicketComboRequest request = new TicketComboRequest(
                comboName,
                "Inactive duplicate test",
                1,
                0,
                0,
                BigDecimal.valueOf(100000),
                true
        );

        String createdComboResponse = mockMvc.perform(post("/api/v1/admin/ticket-pricing/combos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long comboId = objectMapper.readTree(createdComboResponse).at("/data/id").asLong();

        mockMvc.perform(delete("/api/v1/admin/ticket-pricing/combos/{comboId}", comboId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/ticket-pricing/combos")
                        .header("Authorization", "Bearer " + token)
                        .param("active", "false")
                        .param("keyword", comboName)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(comboId))
                .andExpect(jsonPath("$.data.items[0].active").value(false));

        mockMvc.perform(post("/api/v1/admin/ticket-pricing/combos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Combo vé đã tồn tại nhưng đã ngừng hoạt động. Bạn có muốn tạo mới không?"));
    }

    private String loginAsAdmin() throws Exception {
        String password = "Password123";
        User admin = createUser("ticket.admin." + System.nanoTime() + "@example.com", password);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(admin.getEmail(), password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).at("/data/accessToken").asText();
    }

    private User createUser(String email, String password) {
        Role role = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));

        User user = new User(email, passwordEncoder.encode(password), "Ticket Admin", "0900123456");
        user.setEmailVerified(true);
        user.setStatus(com.sba301.cinemaai.enums.UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);
        userRoleRepository.save(new UserRole(savedUser, role));
        return savedUser;
    }
}
