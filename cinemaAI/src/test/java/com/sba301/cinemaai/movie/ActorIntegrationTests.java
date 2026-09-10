package com.sba301.cinemaai.movie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.dto.request.movie.ActorRequest;
import com.sba301.cinemaai.dto.request.movie.MovieCreateRequest;
import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActorIntegrationTests {

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
    void shouldSeparatePublicActorsAndManageActorsThroughMovieActorIds() throws Exception {
        String token = loginAsAdmin();
        String suffix = String.valueOf(System.nanoTime());
        Long linkedActorId = createActor(token, "Linked " + suffix);
        Long unlinkedActorId = createActor(token, "Unused " + suffix);

        mockMvc.perform(get("/api/v1/admin/actors")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "50"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/actors")
                        .param("limit", "50"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/actors")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(get("/api/v1/actors")
                        .param("keyword", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieCreateRequest(
                                "Invalid Main Actor Movie " + suffix,
                                "Main actor must belong to the movie cast.",
                                "https://example.com/trailer",
                                "https://example.com/poster.jpg",
                                "https://example.com/avatar.jpg",
                                100,
                                LocalDate.now(),
                                LocalDate.now().plusDays(30),
                                "English",
                                "Vietnamese",
                                MovieStatus.UPCOMING,
                                "13+",
                                "Actor Test Director",
                                List.of(),
                                List.of(linkedActorId),
                                List.of(unlinkedActorId)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Danh sách diễn viên chính phải nằm trong danh sách diễn viên"));

        Long movieId = createMovie(token, "Actor API Movie " + suffix, linkedActorId);

        mockMvc.perform(get("/api/v1/actors")
                        .param("keyword", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(linkedActorId))
                .andExpect(jsonPath("$.data.items[0].movieCount").value(1));

        mockMvc.perform(get("/api/v1/actors/{actorId}", linkedActorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movieCount").value(1));

        mockMvc.perform(get("/api/v1/actors/{actorId}", unlinkedActorId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/actors/{actorId}/movies", linkedActorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(movieId))
                .andExpect(jsonPath("$.data[0].mainActors").value("Linked " + suffix))
                .andExpect(jsonPath("$.data[0].castList").value("Linked " + suffix));

        String renamedActor = "Renamed " + suffix;
        mockMvc.perform(put("/api/v1/admin/actors/{actorId}", linkedActorId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                renamedActor,
                                "Updated by actor integration test.",
                                "https://example.com/actor-updated.jpg"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(renamedActor));

        mockMvc.perform(get("/api/v1/actors/{actorId}/movies", linkedActorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mainActors").value(renamedActor))
                .andExpect(jsonPath("$.data[0].castList").value(renamedActor));

        mockMvc.perform(delete("/api/v1/admin/actors/{actorId}", linkedActorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/admin/actors/{actorId}", unlinkedActorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/movies/{movieId}/actors", movieId)
                        .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actorIds\":[" + linkedActorId + "]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectActorNameLongerThanFiftyCharacters() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/v1/admin/actors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                "A".repeat(51),
                                "Too long actor name.",
                                "https://example.com/actor.jpg"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void shouldRejectNonImageActorUpload() throws Exception {
        String token = loginAsAdmin();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "actor.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/uploads/images")
                        .file(file)
                        .param("folder", "actors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ cho phép ảnh JPG, PNG và WEBP"));
    }

    private Long createActor(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/actors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActorRequest(
                                name,
                                "Created by actor integration test.",
                                "https://example.com/actor.jpg"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private Long createMovie(String token, String title, Long actorId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieCreateRequest(
                                title,
                                "Actor integration test movie.",
                                "https://example.com/trailer",
                                "https://example.com/poster.jpg",
                                "https://example.com/avatar.jpg",
                                100,
                                LocalDate.now(),
                                LocalDate.now().plusDays(30),
                                "English",
                                "Vietnamese",
                                MovieStatus.NOW_SHOWING,
                                "13+",
                                "Actor Test Director",
                                List.of(),
                                List.of(actorId),
                                List.of(actorId)
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mainActors").value("Linked " + title.substring(title.lastIndexOf(' ') + 1)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private String loginAsAdmin() throws Exception {
        String email = "actor.admin." + System.nanoTime() + "@example.com";
        String password = "Password123";
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));

        User admin = new User(email, passwordEncoder.encode(password), "Actor Admin", "0900111222");
        admin.setEmailVerified(true);
        admin.setStatus(com.sba301.cinemaai.enums.UserStatus.ACTIVE);
        User savedAdmin = userRepository.save(admin);
        userRoleRepository.save(new UserRole(savedAdmin, adminRole));

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(response);
        return loginJson.at("/data/accessToken").asText();
    }
}
