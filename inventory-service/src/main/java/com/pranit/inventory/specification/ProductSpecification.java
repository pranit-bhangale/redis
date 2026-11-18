package com.pranit.inventory.specification;

import com.pranit.inventory.entity.Product;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UtilityClass
public class ProductSpecification {

    public static Specification<Product> filter(
            final String search, final String category,
            final BigDecimal minPrice, final BigDecimal maxPrice
    ) {
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                final String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("productName")), keyword));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase(Locale.ROOT)));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pricePerPiece"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pricePerPiece"), maxPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}