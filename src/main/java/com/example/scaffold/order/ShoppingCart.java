package com.example.scaffold.order;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Browser-session-scoped shopping cart. The REST API stays fully stateless
 * (JWT only); this is purely web-UI state, unrelated to Spring Security's
 * session policy, so it's fine for it to live in the HTTP session.
 */
@Component
@SessionScope
public class ShoppingCart implements Serializable {

    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public void add(Long variationId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        items.merge(variationId, quantity, Integer::sum);
    }

    public void remove(Long variationId) {
        items.remove(variationId);
    }

    public void clear() {
        items.clear();
    }

    public Map<Long, Integer> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getItemCount() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }
}
