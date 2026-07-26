package com.example.scaffold.order;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingCartTest {

    private ShoppingCart cart;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
    }

    @Test
    void newCartIsEmpty() {
        assertTrue(cart.isEmpty());
        assertEquals(0, cart.getItemCount());
    }

    @Test
    void addingItemsAccumulatesQuantity() {
        cart.add(1L, 2);
        cart.add(1L, 3);
        cart.add(2L, 1);

        assertEquals(5, cart.getItems().get(1L));
        assertEquals(1, cart.getItems().get(2L));
        assertEquals(6, cart.getItemCount());
        assertFalse(cart.isEmpty());
    }

    @Test
    void addingNonPositiveQuantityIsIgnored() {
        cart.add(1L, 0);
        cart.add(1L, -5);

        assertTrue(cart.isEmpty());
    }

    @Test
    void removeDropsTheLine() {
        cart.add(1L, 2);
        cart.add(2L, 1);

        cart.remove(1L);

        assertFalse(cart.getItems().containsKey(1L));
        assertTrue(cart.getItems().containsKey(2L));
    }

    @Test
    void clearEmptiesTheCart() {
        cart.add(1L, 2);
        cart.clear();

        assertTrue(cart.isEmpty());
    }
}
