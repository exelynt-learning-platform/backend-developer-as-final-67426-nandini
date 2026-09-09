package com.example.demo.service;

import com.example.demo.dto.PagedResponse;
import com.example.demo.dto.resource.ResourceCreateRequest;
import com.example.demo.dto.resource.ResourceResponse;
import com.example.demo.dto.resource.ResourceUpdateRequest;

public interface ResourceService {
    ResourceResponse create(ResourceCreateRequest request);
    ResourceResponse getById(Long id);
    PagedResponse<ResourceResponse> getAll(int page, int size);
    ResourceResponse update(Long id, ResourceUpdateRequest request);
    void delete(Long id);
}