package com.example.scaffold.order;

import com.example.scaffold.exception.ConflictException;

public class InvalidOrderStateException extends ConflictException {

    public InvalidOrderStateException(Long orderId, OrderStatus currentStatus) {
        super("Order " + orderId + " is already " + currentStatus + " and cannot be updated");
    }
}
