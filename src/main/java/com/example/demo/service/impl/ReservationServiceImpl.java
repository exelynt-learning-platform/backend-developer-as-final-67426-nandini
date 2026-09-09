package com.example.demo.service.impl;

import com.example.demo.dto.PagedResponse;
import com.example.demo.dto.reservation.ReservationCreateRequest;
import com.example.demo.dto.reservation.ReservationResponse;
import com.example.demo.dto.reservation.ReservationUpdateRequest;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.Resource;
import com.example.demo.entity.User;
import com.example.demo.exception.ReservationConflictException;
import com.example.demo.exception.ReservationNotFoundException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedOperationException;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.ResourceRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ReservationService;
import com.example.demo.specification.ReservationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public ReservationResponse create(ReservationCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        if (!resource.isActive()) {
            throw new IllegalArgumentException("Resource is not active");
        }

        validateTimeRange(request.getStartTime(), request.getEndTime());

        checkOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        BigDecimal price = calculatePrice(resource.getPricePerHour(), request.getStartTime(), request.getEndTime());

        Reservation reservation = Reservation.builder()
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(price)
                .status(ReservationStatus.PENDING)
                .user(user)
                .resource(resource)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toResponse(saved);
    }

    @Override
    public ReservationResponse getById(Long id, String username, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));

        if (!isAdmin && !reservation.getUser().getUsername().equals(username)) {
            throw new UnauthorizedOperationException("You can only access your own reservations");
        }

        return reservationMapper.toResponse(reservation);
    }

    @Override
    public PagedResponse<ReservationResponse> getAll(String username, boolean isAdmin,
                                                     ReservationStatus status, BigDecimal minPrice,
                                                     BigDecimal maxPrice, int page, int size, String sort) {
        Specification<Reservation> spec = Specification.where(null);

        if (!isAdmin) {
            spec = spec.and(ReservationSpecification.byUser(username));
        }

        if (status != null) {
            spec = spec.and(ReservationSpecification.byStatus(status));
        }

        if (minPrice != null) {
            spec = spec.and(ReservationSpecification.byMinPrice(minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and(ReservationSpecification.byMaxPrice(maxPrice));
        }

        Pageable pageable = buildPageable(page, size, sort);
        Page<Reservation> reservationPage = reservationRepository.findAll(spec, pageable);
        return reservationMapper.toPagedResponse(reservationPage);
    }

    @Override
    public ReservationResponse update(Long id, ReservationUpdateRequest request, String username, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));

        if (!isAdmin && !reservation.getUser().getUsername().equals(username)) {
            throw new UnauthorizedOperationException("You can only update your own reservations");
        }

        LocalDateTime newStartTime = request.getStartTime() != null ? request.getStartTime() : reservation.getStartTime();
        LocalDateTime newEndTime = request.getEndTime() != null ? request.getEndTime() : reservation.getEndTime();

        validateTimeRange(newStartTime, newEndTime);

        checkOverlap(reservation.getResource().getId(), newStartTime, newEndTime, id);

        // Recalculate price if time changed
        if (request.getStartTime() != null || request.getEndTime() != null) {
            BigDecimal newPrice = calculatePrice(reservation.getResource().getPricePerHour(), newStartTime, newEndTime);
            reservation.setPrice(newPrice);
        }

        // Update status if provided (ADMIN only, checked in controller)
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        reservationMapper.updateEntity(reservation, request);
        Reservation updated = reservationRepository.save(reservation);
        return reservationMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id, String username, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));

        if (!isAdmin && !reservation.getUser().getUsername().equals(username)) {
            throw new UnauthorizedOperationException("You can only delete your own reservations");
        }

        reservationRepository.deleteById(id);
    }

    @Override
    public void cancel(Long id, String username, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));

        if (!isAdmin && !reservation.getUser().getUsername().equals(username)) {
            throw new UnauthorizedOperationException("You can only cancel your own reservations");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start time must be in the future");
        }
    }

    private void checkOverlap(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, Long excludeReservationId) {
        boolean hasOverlap = reservationRepository.existsByResourceIdAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
                resourceId, endTime, startTime, ReservationStatus.CANCELLED);

        if (excludeReservationId != null) {
            List<Reservation> overlapping = reservationRepository.findByResourceIdAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
                    resourceId, endTime, startTime, ReservationStatus.CANCELLED);
            boolean actualOverlap = overlapping.stream()
                    .anyMatch(r -> !r.getId().equals(excludeReservationId));
            if (actualOverlap) {
                throw new ReservationConflictException("Resource is already reserved for the selected time");
            }
        } else if (hasOverlap) {
            throw new ReservationConflictException("Resource is already reserved for the selected time");
        }
    }

    private BigDecimal calculatePrice(BigDecimal pricePerHour, LocalDateTime startTime, LocalDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be >= 0");
        }

        if (sort != null && !sort.isBlank()) {
            String[] sortParams = sort.split(",");
            String property = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;

            String[] allowedProperties = {"price", "startTime", "endTime", "createdAt", "status"};
            boolean allowed = false;
            for (String allowedProp : allowedProperties) {
                if (allowedProp.equals(property)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new IllegalArgumentException("Invalid sort property: " + property + ". Allowed: price, startTime, endTime, createdAt, status");
            }
            return PageRequest.of(page, size, Sort.by(direction, property));
        }
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}