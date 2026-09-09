package com.example.demo;

import com.example.demo.dto.resource.ResourceCreateRequest;
import com.example.demo.dto.reservation.ReservationCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void resourceCreateRequestValidWhenAllFieldsPresent() {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Valid Room", "Description", "ROOM", new BigDecimal("100.00"));

        Set<ConstraintViolation<ResourceCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void resourceCreateRequestInvalidWhenNameMissing() {
        ResourceCreateRequest request = new ResourceCreateRequest(
                null, "Description", "ROOM", new BigDecimal("100.00"));

        Set<ConstraintViolation<ResourceCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void resourceCreateRequestInvalidWhenNameTooShort() {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "A", "Description", "ROOM", new BigDecimal("100.00"));

        Set<ConstraintViolation<ResourceCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void resourceCreateRequestInvalidWhenTypeMissing() {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Valid Room", "Description", null, new BigDecimal("100.00"));

        Set<ConstraintViolation<ResourceCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void resourceCreateRequestInvalidWhenPriceZero() {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Valid Room", "Description", "ROOM", BigDecimal.ZERO);

        Set<ConstraintViolation<ResourceCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pricePerHour")));
    }

    @Test
    void resourceCreateRequestInvalidWhenPriceNegative() {
        ResourceCreateRequest request = new ResourceCreateRequest(
                "Valid Room", "Description", "ROOM", new BigDecimal("-10.00"));

        Set<ConstraintViolation<ResourceCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void reservationCreateRequestValidWhenAllFieldsPresent() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                1L, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));

        Set<ConstraintViolation<ReservationCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void reservationCreateRequestInvalidWhenResourceIdMissing() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                null, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));

        Set<ConstraintViolation<ReservationCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("resourceId")));
    }

    @Test
    void reservationCreateRequestInvalidWhenStartTimeMissing() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                1L, null, LocalDateTime.now().plusHours(2));

        Set<ConstraintViolation<ReservationCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startTime")));
    }

    @Test
    void reservationCreateRequestInvalidWhenEndTimeMissing() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                1L, LocalDateTime.now().plusHours(1), null);

        Set<ConstraintViolation<ReservationCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("endTime")));
    }
}