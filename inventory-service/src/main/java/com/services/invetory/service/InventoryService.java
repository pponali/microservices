package com.services.invetory.service;

import com.services.common.error.exception.ResourceNotFoundException;
import com.services.invetory.dto.InventoryPatchRequest;
import com.services.invetory.dto.InventoryRequest;
import com.services.invetory.dto.InventoryResponse;
import com.services.invetory.model.Inventory;
import com.services.invetory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCode) {
        log.info("Stock check for {} SKU codes", skuCode.size());
        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> findAll(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getById(Long id) {
        return inventoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", id));
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        Inventory item = new Inventory();
        item.setSkuCode(request.getSkuCode());
        item.setQuantity(request.getQuantity());
        Inventory saved = inventoryRepository.save(item);
        log.info("Inventory item {} created with quantity {}", saved.getSkuCode(), saved.getQuantity());
        return toResponse(saved);
    }

    @Transactional
    public InventoryResponse replace(Long id, InventoryRequest request) {
        Inventory existing = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", id));
        existing.setSkuCode(request.getSkuCode());
        existing.setQuantity(request.getQuantity());
        return toResponse(inventoryRepository.save(existing));
    }

    @Transactional
    public InventoryResponse patch(Long id, InventoryPatchRequest patch) {
        Inventory existing = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", id));
        if (patch.getSkuCode() != null) existing.setSkuCode(patch.getSkuCode());
        if (patch.getQuantity() != null) existing.setQuantity(patch.getQuantity());
        return toResponse(inventoryRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory", id);
        }
        inventoryRepository.deleteById(id);
        log.info("Inventory item {} deleted", id);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .skuCode(inventory.getSkuCode())
                .quantity(inventory.getQuantity())
                .isInStock(inventory.getQuantity() != null && inventory.getQuantity() > 0)
                .build();
    }
}
