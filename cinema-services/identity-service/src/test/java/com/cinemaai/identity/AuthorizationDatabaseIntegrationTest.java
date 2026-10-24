package com.cinemaai.identity;

import com.cinemaai.identity.entity.*;
import com.cinemaai.identity.enums.*;
import com.cinemaai.identity.repository.*;
import com.cinemaai.identity.security.JwtService;
import com.cinemaai.identity.service.UserRoleService;
import com.cinemaai.identity.client.CatalogClient;
import com.cinemaai.identity.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP security, services and PostgreSQL; only external Catalog is mocked. */
@EnabledIfSystemProperty(named="authorization.identity.url", matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/identity_auth")
class AuthorizationDatabaseIntegrationTest extends IdentityApplicationSmokeTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url=System.getProperty("authorization.identity.url");
        if(url==null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:(?!5432/)[0-9]+/identity_auth")) throw new IllegalArgumentException("Disposable loopback database required");
        r.add("spring.datasource.url",()->url);
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        r.add("spring.datasource.username",()->"coverage_user");
        r.add("spring.datasource.password",()->"coverage_pass");
        r.add("spring.jpa.hibernate.ddl-auto",()->"create-drop");
    }
    @Autowired MockMvc http;
    @Autowired UserRepository users;
    @Autowired UserRoleService roles;
    @Autowired UserCinemaAssignmentRepository assignments;
    @Autowired AuditLogRepository audit;
    @Autowired JwtService jwt;
    @MockBean CatalogClient catalog;
    User admin,staff,manager,customer;
    @BeforeEach void fixtures() {
        admin=fixture(RoleName.ADMIN); staff=fixture(RoleName.STAFF);
        manager=fixture(RoleName.MANAGER); customer=fixture(RoleName.CUSTOMER);
        assignments.saveAndFlush(new UserCinemaAssignment(staff,101L,admin));
        assignments.saveAndFlush(new UserCinemaAssignment(manager,101L,admin));
    }
    User fixture(RoleName role) {
        User u=new User(UUID.randomUUID()+"@test.invalid","test-hash","Test",UUID.randomUUID().toString().substring(0,18));
        u.setStatus(UserStatus.ACTIVE); u.setEmailVerified(true);
        u=users.saveAndFlush(u); roles.assignRole(u,role); return u;
    }
    org.springframework.http.HttpHeaders headers(User user, String role) {
        var h=new org.springframework.http.HttpHeaders(); h.set("X-Gateway-Secret","gateway-test-secret");
        h.setBearerAuth(jwt.generateAccessToken(user.getId(),user.getEmail(),List.of(role),202L));
        h.set("X-User-Roles","ADMIN"); return h;
    }
    @Test void adminAssignmentPersistsAndInvalidRequestsDoNotMutate() throws Exception {
        long before=audit.count();
        http.perform(put("/api/v1/admin/users/"+staff.getId()+"/cinema").headers(headers(admin,"ADMIN"))
                .contentType("application/json").content("{\"cinemaId\":202}")).andExpect(status().isOk());
        assertEquals(202L,assignments.findByUserId(staff.getId()).orElseThrow().getCinemaId());
        assertEquals(101L,assignments.findByUserId(manager.getId()).orElseThrow().getCinemaId());
        assertTrue(audit.count()>before);
        doThrow(new NotFoundException("missing")).when(catalog).validateActiveCinema(999L);
        doThrow(new BadRequestException("inactive")).when(catalog).validateActiveCinema(303L);
        for(String body:List.of("{\"cinemaId\":999}","{\"cinemaId\":303}","{}","{\"cinemaId\":\"invalid\"}")) {
            http.perform(put("/api/v1/admin/users/"+staff.getId()+"/cinema").headers(headers(admin,"ADMIN"))
                    .contentType("application/json").content(body)).andExpect(status().is4xxClientError());
            assertEquals(202L,assignments.findByUserId(staff.getId()).orElseThrow().getCinemaId());
        }
    }
    @Test void selfReassignmentAndAnonymousFailWithoutDatabaseMutation() throws Exception {
        for(User actor:List.of(staff,manager,customer)) {
            http.perform(put("/api/v1/users/me/cinema").headers(headers(actor,"ADMIN"))
                    .contentType("application/json").content("{\"cinemaId\":202}"))
                    .andExpect(status().isForbidden());
        }
        http.perform(put("/api/v1/users/me/cinema").header("X-Gateway-Secret","gateway-test-secret")
                .contentType("application/json").content("{\"cinemaId\":202}")).andExpect(status().isUnauthorized());
        assertEquals(101L,assignments.findByUserId(staff.getId()).orElseThrow().getCinemaId());
        assertEquals(101L,assignments.findByUserId(manager.getId()).orElseThrow().getCinemaId());
        assertTrue(assignments.findByUserId(customer.getId()).isEmpty());
    }
    @Test void bothStaffAliasesEnforceRoleMatrixAndPreserveAuthoritativeData() throws Exception {
        for(String path:List.of("/api/v1/admin/staff-profiles","/api/v1/staff/profiles")) {
            String id="/"+staff.getId();
            for(User actor:List.of(staff,customer)) {
                for(var request:List.of(get(path),get(path+id),post(path).contentType("application/json").content("{\"userId\":"+staff.getId()+"}"),put(path+id).contentType("application/json").content("{\"position\":\"Bad\"}")))
                    http.perform(request.headers(headers(actor,"ADMIN"))).andExpect(status().isForbidden());
            }
            http.perform(get(path).header("X-Gateway-Secret","gateway-test-secret")).andExpect(status().isUnauthorized());
            for(var request:List.of(get(path+id),post(path).contentType("application/json").content("{\"userId\":"+staff.getId()+"}"),put(path+id).contentType("application/json").content("{\"position\":\"Bad\"}")))
                http.perform(request.header("X-Gateway-Secret","gateway-test-secret")).andExpect(status().isUnauthorized());
            http.perform(post(path).headers(headers(admin,"ADMIN")).contentType("application/json")
                    .content("{\"userId\":"+staff.getId()+",\"position\":\"Created\"}")).andExpect(status().isOk());
            http.perform(put(path+id).headers(headers(admin,"ADMIN")).contentType("application/json")
                    .content("{\"position\":\"Updated\",\"employeeCode\":\"EMP-TEST\"}")).andExpect(status().isOk());
            http.perform(get(path+id).headers(headers(admin,"ADMIN"))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.position").value("Updated"))
                    .andExpect(jsonPath("$.data.email").value(staff.getEmail()));
            http.perform(get(path).headers(headers(admin,"ADMIN"))).andExpect(status().isOk());
            http.perform(get(path+id).headers(headers(admin,"ADMIN"))).andExpect(jsonPath("$.data.position").value("Updated"))
                    .andExpect(jsonPath("$.data.employeeCode").value("EMP-TEST"));
            for(String field:List.of("userId","role","cinemaId","status","email","password"))
                http.perform(put(path+id).headers(headers(admin,"ADMIN")).contentType("application/json")
                        .content("{\""+field+"\":\"forbidden\"}")).andExpect(status().isBadRequest());
            http.perform(get(path+"/99999999").headers(headers(admin,"ADMIN"))).andExpect(status().isNotFound());
            http.perform(post(path).headers(headers(admin,"ADMIN")).contentType("application/json")
                    .content("{\"userId\":99999999}")).andExpect(status().isNotFound());
            assertEquals(UserStatus.ACTIVE,users.findById(staff.getId()).orElseThrow().getStatus());
            assertEquals(101L,assignments.findByUserId(staff.getId()).orElseThrow().getCinemaId());
        }
    }

    @Test void managersManageOnlyStaffInTheirCurrentDatabaseCinema() throws Exception {
        User staffB=fixture(RoleName.STAFF), managerB=fixture(RoleName.MANAGER), unassigned=fixture(RoleName.STAFF);
        assignments.saveAndFlush(new UserCinemaAssignment(staffB,202L,admin));
        assignments.saveAndFlush(new UserCinemaAssignment(managerB,202L,admin));
        for(String path:List.of("/api/v1/admin/staff-profiles","/api/v1/staff/profiles")) {
            // Warm cache as ADMIN before querying with the scoped actor.
            http.perform(get(path).headers(headers(admin,"ADMIN"))).andExpect(status().isOk());
            var scoped=headers(manager,"MANAGER"); scoped.set("X-User-Cinema-Id","202");
            http.perform(get(path).headers(scoped)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[*].id",org.hamcrest.Matchers.hasItem(staff.getId().intValue())))
                    .andExpect(jsonPath("$.data[*].id",org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(staffB.getId().intValue()))));
            http.perform(post(path).headers(scoped).contentType("application/json")
                    .content("{\"userId\":"+staff.getId()+",\"position\":\"Manager-created\"}")).andExpect(status().isOk());
            http.perform(put(path+"/"+staff.getId()).headers(scoped).contentType("application/json")
                    .content("{\"position\":\"Scoped metadata\"}")).andExpect(status().isOk());
            http.perform(get(path+"/"+staff.getId()).headers(scoped)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.position").value("Scoped metadata"));
            for(User target:List.of(staffB,managerB,admin,customer,unassigned)) {
                for(var request:List.of(get(path+"/"+target.getId()),
                        post(path).contentType("application/json").content("{\"userId\":"+target.getId()+"}"),
                        put(path+"/"+target.getId()).contentType("application/json").content("{\"position\":\"Denied\"}")))
                    http.perform(request.headers(scoped)).andExpect(status().isForbidden());
            }
            http.perform(put(path+"/"+staff.getId()).headers(scoped).contentType("application/json")
                    .content("{\"userId\":"+staffB.getId()+"}")).andExpect(status().isBadRequest());
            http.perform(get(path+"/"+staffB.getId()).headers(headers(admin,"ADMIN"))).andExpect(status().isOk());
        }
        // A reassignment in DB immediately removes access even though cached metadata exists.
        var moved=assignments.findByUserId(staff.getId()).orElseThrow(); moved.setCinemaId(202L); assignments.saveAndFlush(moved);
        http.perform(get("/api/v1/staff/profiles/"+staff.getId()).headers(headers(manager,"MANAGER"))).andExpect(status().isForbidden());
    }
}
