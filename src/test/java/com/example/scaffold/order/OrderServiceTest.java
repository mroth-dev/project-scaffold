package com.example.scaffold.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.product.Product;
import com.example.scaffold.product.ProductVariation;
import com.example.scaffold.product.ProductVariationRepository;
import com.example.scaffold.promotion.PromotionRepository;
import com.example.scaffold.promotion.PromotionService;
import com.example.scaffold.user.User;
import com.example.scaffold.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariationRepository productVariationRepository;

    @Mock
    private PromotionService promotionService;

    @Mock
    private PromotionRepository promotionRepository;

    private OrderService orderService;

    private User customer;
    private Product product;
    private ProductVariation variation;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, userRepository, productVariationRepository, promotionService, promotionRepository);

        customer = new User();
        customer.setId(1L);
        customer.setEmail("customer@example.com");

        product = new Product();
        product.setId(1L);
        product.setName("T-Shirt");
        product.setBasePrice(new BigDecimal("19.99"));

        variation = new ProductVariation();
        variation.setId(1L);
        variation.setProduct(product);
        variation.setSku("TSHIRT-001-M-WHITE");
        variation.setInventoryCount(10);
        variation.setPriceAdjustment(BigDecimal.ZERO);
    }

    @Test
    void createOrderComputesTotalAndDecrementsInventory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productVariationRepository.findById(1L)).thenReturn(Optional.of(variation));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 3)));
        OrderDto created = orderService.createOrder(1L, request);

        assertEquals(1L, created.customerId());
        assertEquals(OrderStatus.PENDING, created.status());
        assertEquals(new BigDecimal("59.97"), created.totalAmount());
        assertEquals(1, created.items().size());
        assertEquals("TSHIRT-001-M-WHITE", created.items().get(0).variationSku());
        assertEquals(7, variation.getInventoryCount());
        verify(productVariationRepository, times(1)).save(variation);
    }

    @Test
    void createOrderThrowsWhenInsufficientInventory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productVariationRepository.findById(1L)).thenReturn(Optional.of(variation));

        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 100)));

        assertThrows(InsufficientInventoryException.class, () -> orderService.createOrder(1L, request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderThrowsWhenCustomerMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 1)));

        assertThrows(NotFoundException.class, () -> orderService.createOrder(99L, request));
    }

    @Test
    void getOrderReturnsMappedDto() {
        Order order = buildOrder();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrder(5L);

        assertEquals(5L, result.id());
        assertEquals(1L, result.customerId());
    }

    @Test
    void getOrderThrowsWhenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.getOrder(99L));
    }

    @Test
    void updateOrderStatusAdvancesStatus() {
        Order order = buildOrder();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDto updated = orderService.updateOrderStatus(5L, OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, updated.status());
    }

    @Test
    void updateOrderStatusRestocksOnCancellation() {
        Order order = buildOrder();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateOrderStatus(5L, OrderStatus.CANCELLED);

        assertEquals(12, variation.getInventoryCount());
        verify(productVariationRepository, times(1)).save(variation);
    }

    @Test
    void updateOrderStatusThrowsWhenAlreadyTerminal() {
        Order order = buildOrder();
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.updateOrderStatus(5L, OrderStatus.CANCELLED));
        verify(orderRepository, never()).save(any());
    }

    private Order buildOrder() {
        Order order = new Order();
        order.setId(5L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("39.98"));

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProductVariation(variation);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("19.99"));
        item.setSubtotal(new BigDecimal("39.98"));
        order.getItems().add(item);

        return order;
    }
}
