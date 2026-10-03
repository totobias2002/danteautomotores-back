package com.danteautomotores.repository.spec;

import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.OrdenCatalogo;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Consultas del catálogo público. Todo el texto del cliente viaja como parámetro enlazado (Criteria API): nunca se
 * concatena SQL. Los filtros que llegan acá ya pasaron por CatalogoService.normalizar (límites de cantidad y largo).
 */
public class CatalogoSpecification {

    private CatalogoSpecification() {
    }

    /**
     * Autos que el público puede ver: todo menos los vendidos, y los vendidos solo si se marcaron hace poco. Un
     * VENDIDO sin fecha de venta queda oculto (falla seguro: la comparación con NULL no da verdadero).
     */
    public static Specification<Publicacion> visible(LocalDateTime limiteVendidos) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("estado")),
                cb.notEqual(root.get("estado"), EstadoPublicacion.VENDIDO),
                cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaVendido"), limiteVendidos));
    }

    /** AND entre filtros y OR dentro de cada lista. Asume filtros normalizados. */
    public static Specification<Publicacion> conFiltros(FiltrosCatalogo filtros) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (filtros.getBusqueda() != null && !filtros.getBusqueda().isBlank()) {
                // Cada palabra debe aparecer en "marca modelo"; % _ y \ del cliente son texto, no comodines.
                Expression<String> marcaYModelo = cb.lower(cb.concat(cb.concat(root.<String>get("marca"), " "), root.<String>get("modelo")));
                for (String palabra : filtros.getBusqueda().trim().toLowerCase(Locale.ROOT).split("\\s+")) {
                    predicados.add(cb.like(marcaYModelo, "%" + escaparLike(palabra) + "%", '\\'));
                }
            }
            agregarTextoExacto(predicados, cb.lower(root.<String>get("marca")), filtros.getMarca());
            agregarTextoExacto(predicados, cb.lower(root.<String>get("modelo")), filtros.getModelo());
            agregarTextoExacto(predicados, cb.lower(root.<String>get("color")), filtros.getColor());

            if (hay(filtros.getTransmision())) {
                predicados.add(root.get("transmision").in(filtros.getTransmision()));
            }
            if (hay(filtros.getTipo())) {
                predicados.add(root.get("tipoCarroceria").in(filtros.getTipo()));
            }
            if (hay(filtros.getZona())) {
                predicados.add(root.get("agencia").get("zona").in(filtros.getZona()));
            }
            if (hay(filtros.getEstado())) {
                predicados.add(root.get("estado").in(filtros.getEstado()));
            }
            if (filtros.getAnioMin() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<Integer>get("anio"), filtros.getAnioMin()));
            }
            if (filtros.getAnioMax() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<Integer>get("anio"), filtros.getAnioMax()));
            }
            if (filtros.getKmMax() != null) {
                // Un auto sin kilometraje cargado no cumple "hasta X km".
                predicados.add(cb.isNotNull(root.get("kilometraje")));
                predicados.add(cb.lessThanOrEqualTo(root.<Integer>get("kilometraje"), filtros.getKmMax()));
            }
            if (filtros.getPrecioMin() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("precio"), filtros.getPrecioMin()));
            }
            if (filtros.getPrecioMax() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("precio"), filtros.getPrecioMax()));
            }
            if (Boolean.TRUE.equals(filtros.getOfertas())) {
                // La misma regla que PublicacionMapper.esOferta: precio anterior mayor que el precio actual.
                predicados.add(cb.isNotNull(root.get("precioAnterior")));
                predicados.add(cb.greaterThan(root.<BigDecimal>get("precioAnterior"), root.<BigDecimal>get("precio")));
            }
            if (filtros.getAgenciaId() != null) {
                predicados.add(cb.equal(root.get("agencia").get("id"), filtros.getAgenciaId()));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /**
     * Orden del listado. Primero los vendidos al final (D-04), después el criterio pedido y SIEMPRE id descendente al
     * final: sin un desempate total, dos páginas consecutivas pueden repetir o saltear autos. No ordena la consulta de
     * conteo (donde el tipo de resultado es Long).
     */
    public static Specification<Publicacion> conOrden(OrdenCatalogo orden) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                List<Order> criterios = new ArrayList<>();
                criterios.add(cb.asc(cb.selectCase().when(cb.equal(root.get("estado"), EstadoPublicacion.VENDIDO), 1).otherwise(0)));
                switch (orden) {
                    case PRECIO_ASC -> criterios.add(cb.asc(root.get("precio")));
                    case PRECIO_DESC -> criterios.add(cb.desc(root.get("precio")));
                    case ANIO_DESC -> criterios.add(cb.desc(root.get("anio")));
                    // En PostgreSQL los NULL van al final en ASC: los autos sin km quedan al final.
                    case KM_ASC -> criterios.add(cb.asc(root.get("kilometraje")));
                    default -> {
                        criterios.add(cb.desc(root.get("destacado")));
                        criterios.add(cb.desc(root.get("fechaPublicacion")));
                    }
                }
                criterios.add(cb.desc(root.get("id")));
                query.orderBy(criterios);
            }
            return null;
        };
    }

    /**
     * Autos parecidos a uno dado (D-06): DISPONIBLES, distintos del auto, en su misma moneda, con precio entre el 70 % y
     * el 130 % del suyo y del mismo tipo de carrocería o de la misma marca (solo la marca si el auto no tiene tipo).
     * Ordenados por cercanía de precio y, a igual distancia, el más nuevo (id desc). Sirve para un auto en cualquier
     * estado, incluso un vendido hace meses.
     */
    public static Specification<Publicacion> similaresA(Publicacion base) {
        BigDecimal precioBase = base.getPrecio();
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("estado"), EstadoPublicacion.DISPONIBLE));
            predicados.add(cb.notEqual(root.get("id"), base.getId()));
            if (base.getMoneda() == null) {
                predicados.add(cb.isNull(root.get("moneda")));
            } else {
                predicados.add(cb.equal(root.get("moneda"), base.getMoneda()));
            }
            predicados.add(cb.between(root.<BigDecimal>get("precio"),
                    precioBase.multiply(new BigDecimal("0.70")), precioBase.multiply(new BigDecimal("1.30"))));

            Predicate mismaMarca = cb.equal(cb.lower(root.<String>get("marca")), base.getMarca().trim().toLowerCase(Locale.ROOT));
            if (base.getTipoCarroceria() == null) {
                predicados.add(mismaMarca);
            } else {
                predicados.add(cb.or(cb.equal(root.get("tipoCarroceria"), base.getTipoCarroceria()), mismaMarca));
            }

            // No ordena la consulta de conteo (donde el tipo de resultado es Long).
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(
                        cb.asc(cb.abs(cb.diff(root.<BigDecimal>get("precio"), precioBase))),
                        cb.desc(root.get("id")));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static boolean hay(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }

    private static void agregarTextoExacto(List<Predicate> predicados, Expression<String> columna, List<String> valores) {
        if (hay(valores)) {
            predicados.add(columna.in(valores.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList()));
        }
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
