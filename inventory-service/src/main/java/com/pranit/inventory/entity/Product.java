package com.pranit.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_product_name", columnList = "product_name"),
                @Index(name = "idx_products_category", columnList = "category")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_products_product_name_category",
                        columnNames = {"product_name", "category"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerPiece;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer remaining;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    @Version
    private Long version;

    public void calculateTotalPrice() {
        this.totalPrice = pricePerPiece.multiply(BigDecimal.valueOf(totalQuantity));
    }
}