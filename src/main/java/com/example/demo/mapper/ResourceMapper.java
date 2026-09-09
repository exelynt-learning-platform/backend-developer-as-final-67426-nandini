package com.example.demo.mapper;

import com.example.demo.dto.resource.ResourceCreateRequest;
import com.example.demo.dto.resource.ResourceResponse;
import com.example.demo.dto.resource.ResourceUpdateRequest;
import com.example.demo.entity.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    ResourceMapper INSTANCE = Mappers.getMapper(ResourceMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    Resource toEntity(ResourceCreateRequest request);

    ResourceResponse toResponse(Resource resource);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Resource resource, ResourceUpdateRequest request);

    default PagedResponse<ResourceResponse> toPagedResponse(Page<Resource> page) {
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