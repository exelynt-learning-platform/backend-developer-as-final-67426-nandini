package com.example.demo.mapper;

import com.example.demo.dto.PagedResponse;
import com.example.demo.dto.reservation.ReservationCreateRequest;
import com.example.demo.dto.reservation.ReservationResponse;
import com.example.demo.dto.reservation.ReservationUpdateRequest;
import com.example.demo.dto.resource.ResourceResponse;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.Resource;
import com.example.demo.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationMapper INSTANCE = Mappers.getMapper(ReservationMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "resource", ignore = true)
    @Mapping(target = "status", constant = "PENDING", qualifiedByName = "mapStatus")
    Reservation toEntity(ReservationCreateRequest request);

    default ReservationStatus mapStatus(String status) {
        return ReservationStatus.PENDING;
    }

    @Mapping(target = "resource", expression = "java(toResourceResponse(reservation.getResource()))")
    @Mapping(target = "userId", expression = "java(reservation.getUser().getId())")
    @Mapping(target = "username", expression = "java(reservation.getUser().getUsername())")
    @Mapping(target = "status", source = "status")
    ReservationResponse toResponse(Reservation reservation);

    @BeanMapping(nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "resource", ignore = true)
    void updateEntity(@MappingTarget Reservation reservation, ReservationUpdateRequest request);

    default ResourceResponse toResourceResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .pricePerHour(resource.getPricePerHour())
                .active(resource.isActive())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }

    default PagedResponse<ReservationResponse> toPagedResponse(Page<Reservation> page) {
        return new com.example.demo.dto.PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}