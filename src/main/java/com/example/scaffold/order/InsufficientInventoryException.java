package com.example.scaffold.order;

import com.example.scaffold.exception.ConflictException;

public class InsufficientInventoryException extends ConflictException {

    public InsufficientInventoryException(String sku, int requested, int available) {
        super("Insufficient inventory for variation " + sku
                + ": requested " + requested + ", available " + available);
    }
}
