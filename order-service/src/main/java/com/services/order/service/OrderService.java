package com.services.order.service;


import com.services.common.error.exception.ResourceNotFoundException;
import com.services.order.dto.InventoryResponse;
import com.services.order.dto.OrderLineItemsDto;
import com.services.order.dto.OrderLineItemsResponse;
import com.services.order.dto.OrderPatchRequest;
import com.services.order.dto.OrderRequest;
import com.services.order.dto.OrderResponse;
import com.services.order.event.OrderPlacedEvent;
import com.services.order.model.Order;
import com.services.order.model.OrderLineItems;
import com.services.order.repository.OrderRepository;
import io.lettuce.core.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        Tracer.Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");

        try {
            inventoryServiceLookup.tag("call", "inventory-service");
            InventoryResponse[] inventoryResponsArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponsArray)
                    .allMatch(InventoryResponse::isInStock);

            if (allProductsInStock) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order Placed Successfully";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        } finally {
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    public OrderResponse replaceOrder(Long id, OrderRequest request) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        List<OrderLineItems> newItems = request.getOrderLineItemsDtoList().stream()
                .map(this::mapToDto)
                .toList();
        existing.setOrderLineItemsList(newItems);
        return toResponse(orderRepository.save(existing));
    }

    public OrderResponse patchOrder(Long id, OrderPatchRequest patch) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        if (patch.getItems() != null) {
            existing.setOrderLineItemsList(
                    patch.getItems().stream().map(this::mapToDto).toList());
        }
        return toResponse(orderRepository.save(existing));
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", id);
        }
        orderRepository.deleteById(id);
    }

    private OrderLineItems mapToDto(OrderLineItemsDto dto) {
        OrderLineItems item = new OrderLineItems();
        item.setPrice(dto.getPrice());
        item.setQuantity(dto.getQuantity());
        item.setSkuCode(dto.getSkuCode());
        return item;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderLineItemsResponse> items = order.getOrderLineItemsList() == null
                ? List.of()
                : order.getOrderLineItemsList().stream()
                        .map(i -> OrderLineItemsResponse.builder()
                                .id(i.getId())
                                .skuCode(i.getSkuCode())
                                .price(i.getPrice())
                                .quantity(i.getQuantity())
                                .build())
                        .toList();
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .items(items)
                .build();
    }
}
