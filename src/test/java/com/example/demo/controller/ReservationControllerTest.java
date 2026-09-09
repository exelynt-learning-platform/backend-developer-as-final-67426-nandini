package com.example.demo.controller;

import com.example.demo.dto.reservation.ReservationCreateRequest;
import com.example.demo.dto.reservation.ReservationUpdateRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String userToken;
    private String user2Token;
    private User admin;
    private User user;
    private User user2;
    private Resource resource;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("User@123"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        user2 = User.builder()
                .username("user2")
                .password(passwordEncoder.encode("User@123"))
                .role(Role.USER)
                .build();
        userRepository.save(user2);

        adminToken = jwtService.generateToken(admin);
        userToken = jwtService.generateToken(user);
        user2Token = jwtService.generateToken(user2);

        resource = Resource.builder()
                .name("Test Room")
                .description("A test room")
                .type("ROOM")
                .pricePerHour(new BigDecimal("100.00"))
                .active(true)
                .build();
        resource = resourceRepository.save(resource);
    }

    @Test
    void userCanCreateReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        ReservationCreateRequest request = new ReservationCreateRequest(resource.getId(), start, end);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resource.id").value(resource.getId()))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void reservationOwnerComesFromJWT() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        ReservationCreateRequest request = new ReservationCreateRequest(resource.getId(), start, end);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void userCannotImpersonateAnotherUser() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        ReservationCreateRequest request = new ReservationCreateRequest(resource.getId(), start, end);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void userSeesOnlyOwnReservations() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        // User creates reservation
        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        // User2 creates reservation
        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start.plusHours(3), end.plusHours(3)))))
                .andExpect(status().isCreated());

        // User lists - should see only 1
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void userCanRetrieveOwnReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId));
    }

    @Test
    void userCannotRetrieveAnotherUsersReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotUpdateReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        ReservationUpdateRequest updateRequest = new ReservationUpdateRequest(
                start.plusHours(1), end.plusHours(1), null);

        mockMvc.perform(put("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCancelReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewAllReservations() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start.plusHours(3), end.plusHours(3)))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminCanRetrieveAnyReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUpdateReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        ReservationUpdateRequest updateRequest = new ReservationUpdateRequest(
                start.plusHours(1), end.plusHours(1), null);

        mockMvc.perform(put("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanCancelReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingRequiredFieldReturns400() throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest(null, null, null);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endTimeBeforeStartTimeReturns400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingReservationReturns409() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start.plusMinutes(30), end.plusMinutes(30)))))
                .andExpect(status().isConflict());
    }

    @Test
    void nonOverlappingReservationSucceeds() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), end.plusHours(1), end.plusHours(2)))))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelledReservationDoesNotBlockSlot() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());
    }

    @Test
    void filterByStatusWorks() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filterByMinPriceWorks() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("minPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("minPrice", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filterByMaxPriceWorks() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("maxPrice", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("maxPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void combinedFiltersWork() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "PENDING")
                        .param("minPrice", "50")
                        .param("maxPrice", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void paginationReturnsCorrectPageSize() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/reservations")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ReservationCreateRequest(resource.getId(),
                                            start.plusHours(i * 3), end.plusHours(i * 3)))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void invalidPageSizeReturns400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidNegativePageReturns400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sortingByPriceWorks() throws Exception {
        Resource resource2 = Resource.builder()
                .name("Expensive Room")
                .description("Expensive")
                .type("ROOM")
                .pricePerHour(new BigDecimal("500.00"))
                .active(true)
                .build();
        resourceRepository.save(resource2);

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource2.getId(), start.plusHours(3), end.plusHours(3)))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(100.00))
                .andExpect(jsonPath("$.content[1].price").value(500.00));
    }

    @Test
    void invalidSortPropertyReturns400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("sort", "password,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanChangeReservationStatus() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();

        ReservationUpdateRequest updateRequest = new ReservationUpdateRequest(
                null, null, ReservationStatus.CONFIRMED);

        mockMvc.perform(put("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void priceRecalculatedWhenTimeChanges() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservationCreateRequest(resource.getId(), start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(response).get("id").asLong();
        // Initial price: 1 hour * 100 = 100
        BigDecimal initialPrice = objectMapper.readTree(response).get("price").decimalValue();

        // Admin updates to 3 hours
        LocalDateTime newStart = LocalDateTime.now().plusHours(1);
        LocalDateTime newEnd = LocalDateTime.now().plusHours(4);

        ReservationUpdateRequest updateRequest = new ReservationUpdateRequest(newStart, newEnd, null);

        String updateResponse = mockMvc.perform(put("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        BigDecimal updatedPrice = objectMapper.readTree(updateResponse).get("price").decimalValue();
        // New price: 3 hours * 100 = 300
        assert updatedPrice.compareTo(new BigDecimal("300.00")) == 0;
    }
}