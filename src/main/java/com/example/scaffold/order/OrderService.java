package com.example.scaffold.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.scaffold.config.CacheConfig;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.product.ProductVariation;
import com.example.scaffold.product.ProductVariationRepository;
import com.example.scaffold.promotion.PromotionQuote;
import com.example.scaffold.promotion.PromotionRepository;
import com.example.scaffold.promotion.PromotionService;
import com.example.scaffold.user.User;
import com.example.scaffold.user.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductVariationRepository productVariationRepository;
    private final PromotionService promotionService;
    private final PromotionRepository promotionRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
            ProductVariationRepository productVariationRepository, PromotionService promotionService,
            PromotionRepository promotionRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productVariationRepository = productVariationRepository;
        this.promotionService = promotionService;
        this.promotionRepository = promotionRepository;
    }

    public OrderDto createOrder(Long customerId, OrderRequest request) {
        log.debug("Creating order for customer: {}", customerId);
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("User", customerId));

        Order order = new Order();
        order.setCustomer(customer);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            ProductVariation variation = productVariationRepository.findById(itemRequest.productVariationId())
                    .orElseThrow(() -> new NotFoundException("ProductVariation", itemRequest.productVariationId()));

            if (variation.getInventoryCount() < itemRequest.quantity()) {
                throw new InsufficientInventoryException(
                        variation.getSku(), itemRequest.quantity(), variation.getInventoryCount());
            }
            variation.setInventoryCount(variation.getInventoryCount() - itemRequest.quantity());
            productVariationRepository.save(variation);

            BigDecimal unitPrice = variation.getProduct().getBasePrice().add(variation.getPriceAdjustment());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductVariation(variation);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(lineTotal);
            order.getItems().add(item);

            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        PromotionQuote quote = null;
        if (request.promotionCode() != null && !request.promotionCode().isBlank()) {
            quote = promotionService.quote(request.promotionCode(), subtotal);
            promotionService.assertCustomerCanRedeem(quote.promotionId(), customerId);
            discount = quote.discountAmount();
            Long promotionId = quote.promotionId();
            order.setPromotion(promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new NotFoundException("Promotion", promotionId)));
        }
        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.subtract(discount));

        Order savedOrder = orderRepository.save(order);

        if (quote != null) {
            promotionService.redeem(quote.promotionId(), customerId, savedOrder);
        }

        return toDto(savedOrder);
    }

    @Cacheable(value = CacheConfig.ORDER_CACHE, key = "#orderId")
    public OrderDto getOrder(Long orderId) {
        log.debug("Fetching order: {}", orderId);
        return toDto(findOrderOrThrow(orderId));
    }

    public Page<OrderDto> getOrdersForCustomer(Long customerId, Pageable pageable) {
        log.debug("Fetching orders for customer: {}", customerId);
        return orderRepository.findByCustomerId(customerId, pageable).map(this::toDto);
    }

    public Page<OrderDto> getOrders(OrderStatus status, Pageable pageable) {
        log.debug("Fetching orders with status: {}", status);
        Page<Order> orders = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return orders.map(this::toDto);
    }

    @CacheEvict(value = CacheConfig.ORDER_CACHE, key = "#orderId")
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.debug("Updating order {} status to {}", orderId, newStatus);
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(orderId, order.getStatus());
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restockItems(order);
        }

        order.setStatus(newStatus);
        return toDto(orderRepository.save(order));
    }

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order", orderId));
    }

    private void restockItems(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariation variation = item.getProductVariation();
            variation.setInventoryCount(variation.getInventoryCount() + item.getQuantity());
            productVariationRepository.save(variation);
        }
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getId(),
                        item.getProductVariation().getId(),
                        item.getProductVariation().getProduct().getName(),
                        item.getProductVariation().getSku(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();
        return new OrderDto(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPromotion() != null ? order.getPromotion().getCode() : null,
                order.getDiscountAmount(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
