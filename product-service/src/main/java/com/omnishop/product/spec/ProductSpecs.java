package com.omnishop.product.spec;

import com.omnishop.product.model.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.*;

/**
 * Composable JPA Specifications — filters become parameterized predicates,
 * never concatenated SQL strings (no injection surface).
 */
public final class ProductSpecs {

    private ProductSpecs() {}

    public static Specification<Product> textSearch(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("category")), like));
        };
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (min != null) preds.add(cb.greaterThanOrEqualTo(root.get("price"), min));
            if (max != null) preds.add(cb.lessThanOrEqualTo(root.get("price"), max));
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> categoryIs(String category) {
        return (root, query, cb) ->
                (category == null || category.isBlank())
                        ? cb.conjunction()
                        : cb.equal(root.get("category"), category);
    }
}
