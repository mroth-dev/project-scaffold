package com.example.scaffold.product;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variations")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String size;

    private String color;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "inventory_count", nullable = false)
    private int inventoryCount;

    @Column(name = "price_adjustment", nullable = false)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;
}
