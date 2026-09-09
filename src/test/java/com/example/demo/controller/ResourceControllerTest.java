package com.example.demo.controller;

import com.example.demo.dto.resource.ResourceCreateRequest;
import com.example.demo.entity.Resource;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.ResourceRepository;
import com.example.demo.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private Resource testResource;

    @BeforeEach
    void setup() {
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        User user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("User@123"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        adminToken = jwtService.generateToken(admin);
        userToken = jwtService.generateToken(user);

        testResource = Resource.builder()
                .name("Test Room")
                .description("A test room")
                .type("ROOM")
                .pricePerHour(new BigDecimal("100.00"))
                .active(true)
                .build();
        testResource = resourceRepository.save(testResource);
    }

    @Test
    void userCanListResources() throws Exception {
        mockMvc.perform(get("/resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void userCanRetrieveResource() throws Exception {
        mockMvc.perform(get("/resources/" + testResource.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testResource.getId()))
                .andExpect(jsonPath("$.name").value("Test Room"));
    }

    @Test
    void userCannotCreateResource() throws Exception {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "New Room", "Description", "ROOM", new BigDecimal("200.00"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotUpdateResource() throws Exception {
        mockMvc.perform(put("/resources/" + testResource.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResourceCreateRequest(
                                "Updated", "Desc", "ROOM", new BigDecimal("300.00")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteResource() throws Exception {
        mockMvc.perform(delete("/resources/" + testResource.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateResource() throws Exception {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Admin Room", "Admin created", "ROOM", new BigDecimal("500.00"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Admin Room"));
    }

    @Test
    void adminCanUpdateResource() throws Exception {
        mockMvc.perform(put("/resources/" + testResource.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResourceCreateRequest(
                                "Updated Room", "Updated", "ROOM", new BigDecimal("600.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Room"));
    }

    @Test
    void adminCanDeleteResource() throws Exception {
        mockMvc.perform(delete("/resources/" + testResource.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/resources/" + testResource.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized());
    }
}