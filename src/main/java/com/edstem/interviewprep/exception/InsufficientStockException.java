package com.edstem.interviewprep.exception;


public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(Long productId, String productName, int requested) {
        super("Insufficient stock for product '" + productName + "' (id " + productId + "): requested "
                + requested + ". The order was not placed and no stock was reserved.");
    }
}
