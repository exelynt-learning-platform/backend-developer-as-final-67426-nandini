package com.example.demo.service.impl;

import com.example.demo.dto.PagedResponse;
import com.example.demo.dto.resource.ResourceCreateRequest;
import com.example.demo.dto.resource.ResourceResponse;
import com.example.demo.dto.resource.ResourceUpdateRequest;
import com.example.demo.entity.Resource;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.ResourceMapper;
import com.example.demo.repository.ResourceRepository;
import com.example.demo.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    @Override
    public ResourceResponse create(ResourceCreateRequest request) {
        Resource resource = resourceMapper.toEntity(request);
        Resource saved = resourceRepository.save(resource);
        return resourceMapper.toResponse(saved);
    }

    @Override
    public ResourceResponse getById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
        return resourceMapper.toResponse(resource);
    }

    @Override
    public PagedResponse<ResourceResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Resource> resourcePage = resourceRepository.findAll(pageable);
        return resourceMapper.toPagedResponse(resourcePage);
    }

    @Override
    public ResourceResponse update(Long id, ResourceUpdateRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        resourceMapper.updateEntity(resource, request);
        Resource updated = resourceRepository.save(resource);
        return resourceMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        resourceRepository.deleteById(id);
    }
}