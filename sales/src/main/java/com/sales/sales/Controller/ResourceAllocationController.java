package com.sales.sales.Controller;

import com.sales.sales.Entity.ResourceAllocation;
import com.sales.sales.Services.ResourceAllocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/allocations")
@CrossOrigin(origins = "*")
public class ResourceAllocationController {

    private final ResourceAllocationService service;

    public ResourceAllocationController(ResourceAllocationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ResourceAllocation> getAllAllocations() {
        return service.getAllAllocations();
    }

    @GetMapping("/{id}")
    public Optional<ResourceAllocation> getAllocationById(@PathVariable Long id) {
        return service.getAllocationById(id);
    }

    @PostMapping
    public ResourceAllocation addAllocation(@RequestBody ResourceAllocation allocation) {
        return service.addAllocation(allocation);
    }

    @PutMapping("/{id}")
    public ResourceAllocation updateAllocation(@PathVariable Long id, @RequestBody ResourceAllocation allocation) {
        return service.updateAllocation(id, allocation);
    }

    @DeleteMapping("/{id}")
    public void deleteAllocation(@PathVariable Long id) {
        service.deleteAllocation(id);
    }
}
