package com.danteautomotores.repository.spec;

import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.EstadoPublicacion;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class PublicacionSpecification {

    private PublicacionSpecification() {
    }

    public static Specification<Publicacion> conFiltros(String marca, String modelo, Integer anioMin, Integer anioMax,
                                                          BigDecimal precioMin, BigDecimal precioMax,
                                                          EstadoPublicacion estado, Long agenciaId) {
        return (root, query, cb) -> {
            Predicate predicado = cb.conjunction();

            if (marca != null && !marca.isBlank()) {
                predicado = cb.and(predicado, cb.like(cb.lower(root.get("marca")), "%" + marca.toLowerCase() + "%"));
            }
            if (modelo != null && !modelo.isBlank()) {
                predicado = cb.and(predicado, cb.like(cb.lower(root.get("modelo")), "%" + modelo.toLowerCase() + "%"));
            }
            if (anioMin != null) {
                predicado = cb.and(predicado, cb.greaterThanOrEqualTo(root.get("anio"), anioMin));
            }
            if (anioMax != null) {
                predicado = cb.and(predicado, cb.lessThanOrEqualTo(root.get("anio"), anioMax));
            }
            if (precioMin != null) {
                predicado = cb.and(predicado, cb.greaterThanOrEqualTo(root.get("precio"), precioMin));
            }
            if (precioMax != null) {
                predicado = cb.and(predicado, cb.lessThanOrEqualTo(root.get("precio"), precioMax));
            }
            if (estado != null) {
                predicado = cb.and(predicado, cb.equal(root.get("estado"), estado));
            }
            if (agenciaId != null) {
                predicado = cb.and(predicado, cb.equal(root.get("agencia").get("id"), agenciaId));
            }

            return predicado;
        };
    }
}
