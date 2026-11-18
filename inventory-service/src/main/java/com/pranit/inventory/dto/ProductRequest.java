package com.pranit.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(

        @Size(max = 150, message = "Product name cannot exceed 150 characters")
        String productName,

        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category,

        @NotNull(message = "Price per piece is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        BigDecimal pricePerPiece,

        @NotNull(message = "Total quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer totalQuantity
) {
}
