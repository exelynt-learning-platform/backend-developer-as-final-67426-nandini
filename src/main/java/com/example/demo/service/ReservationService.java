package com.example.demo.service;

import com.example.demo.dto.PagedResponse;
import com.example.demo.dto.reservation.ReservationCreateRequest;
import com.example.demo.dto.reservation.ReservationResponse;
import com.example.demo.dto.reservation.ReservationUpdateRequest;
import com.example.demo.entity.ReservationStatus;

import java.math.BigDecimal;

public interface ReservationService {
    ReservationResponse create(ReservationCreateRequest request, String username);
    ReservationResponse getById(Long id, String username, boolean isAdmin);
    PagedResponse<ReservationResponse> getAll(String username, boolean isAdmin, ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice, int page, int size, String sort);
    ReservationResponse update(Long id, ReservationUpdateRequest request, String username, boolean isAdmin);
    void delete(Long id, String username, boolean isAdmin);
    void cancel(Long id, String username, boolean isAdmin);
}