package com.example.demo.controller;

import com.example.demo.dto.PagedResponse;
import com.example.demo.dto.reservation.ReservationCreateRequest;
import com.example.demo.dto.reservation.ReservationResponse;
import com.example.demo.dto.reservation.ReservationUpdateRequest;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reservation management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a new reservation")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request, Authentication authentication) {
        String username = authentication.getName();
        ReservationResponse response = reservationService.create(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all reservations with filtering, pagination, and sorting")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagedResponse<ReservationResponse>> getAll(
            @Parameter(description = "Filter by status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field,direction (e.g., price,asc or startTime,desc)") @RequestParam(required = false) String sort,
            Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(reservationService.getAll(username, isAdmin, status, minPrice, maxPrice, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(reservationService.getById(id, username, isAdmin));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody ReservationUpdateRequest request,
                                                      Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(reservationService.update(id, request, username, isAdmin));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reservationService.delete(id, username, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reservationService.cancel(id, username, isAdmin);
        return ResponseEntity.noContent().build();
    }
}