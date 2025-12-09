package com.sales.sales.Services;


import com.sales.sales.Entity.ResourceAllocation;
import com.sales.sales.Repositories.ResourceAllocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResourceAllocationService {

    private final ResourceAllocationRepository repo;

    public ResourceAllocationService(ResourceAllocationRepository repo) {
        this.repo = repo;
    }

    public List<ResourceAllocation> getAllAllocations() {
        return repo.findAll();
    }

    public Optional<ResourceAllocation> getAllocationById(Long id) {
        return repo.findById(id);
    }

    public ResourceAllocation addAllocation(ResourceAllocation allocation) {
        return repo.save(allocation);
    }

    public ResourceAllocation updateAllocation(Long id, ResourceAllocation allocation) {
        if (repo.existsById(id)) {
            allocation.setAllocationId(id);
            return repo.save(allocation);
        }
        return null;
    }

    public void deleteAllocation(Long id) {
        repo.deleteById(id);
    }
}
