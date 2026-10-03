package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.FacetasResponse;
import com.danteautomotores.dto.publicacion.FacetasResponse.Conteo;
import com.danteautomotores.dto.publicacion.FacetasResponse.ConteoModelo;
import com.danteautomotores.dto.publicacion.FacetasResponse.Rango;
import com.danteautomotores.dto.publicacion.FacetasResponse.RangoPrecio;
import com.danteautomotores.dto.publicacion.FacetasResponse.TramoPrecio;
import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.OrdenCatalogo;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.PublicacionMapper;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.spec.CatalogoSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Lectura del catálogo público (sin token). Es una clase aparte de PublicacionService, que concentra las escrituras
 * del admin; acá solo se devuelven DTOs resumidos, sin datos de usuarios.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogoService {

    static final int LIMITE_DESTACADOS_POR_DEFECTO = 6;
    static final int LIMITE_DESTACADOS_MAXIMO = 12;

    static final int LIMITE_SIMILARES_POR_DEFECTO = 4;
    static final int LIMITE_SIMILARES_MAXIMO = 8;

    // Tamaño de página fijo (D-07): el cliente no lo elige.
    static final int TAMANIO_PAGINA = 24;
    // 240.000 autos: muy por encima de cualquier inventario real, y con offset holgado dentro de un int.
    static final int MAX_PAGINA = 10_000;
    static final int MAX_VALORES_POR_FILTRO = 20;
    static final int MAX_LARGO_BUSQUEDA = 60;
    // Los mismos tramos que calculaba el mock del front.
    static final int CANTIDAD_TRAMOS_PRECIO = 16;

    private final PublicacionRepository publicacionRepository;
    private final Clock clock;

    // El inicializador sirve para los tests sin Spring.
    @Value("${app.catalogo.dias-vendido-visible:30}")
    private int diasVendidoVisible = 30;

    public List<PublicacionResumenResponse> destacados(Integer limite) {
        // El límite nunca lo decide un tamaño arbitrario del cliente: se acota a 1..12.
        int tope = limite == null ? LIMITE_DESTACADOS_POR_DEFECTO
                : Math.min(Math.max(limite, 1), LIMITE_DESTACADOS_MAXIMO);
        // El mapeo va adentro de la transacción: open-in-view está apagado y agencia/fotos son lazy.
        return publicacionRepository.findDestacadosVisibles(EstadoPublicacion.VENDIDO, PageRequest.of(0, tope))
                .stream()
                .map(PublicacionMapper::toResumen)
                .toList();
    }

    public PaginaResponse<PublicacionResumenResponse> buscar(FiltrosCatalogo crudos) {
        FiltrosCatalogo filtros = normalizar(crudos);
        Specification<Publicacion> spec = Specification.where(CatalogoSpecification.visible(limiteVendidosVisibles()))
                .and(CatalogoSpecification.conFiltros(filtros))
                .and(CatalogoSpecification.conOrden(OrdenCatalogo.desde(filtros.getOrden())));
        // Sin Sort en el PageRequest: el orden (con desempate por id) lo arma la especificación.
        Page<Publicacion> pagina = publicacionRepository.findAll(spec, PageRequest.of(filtros.getPagina() - 1, TAMANIO_PAGINA));
        // El mapeo va adentro de la transacción: agencia y fotos son lazy.
        return PaginaResponse.de(pagina.map(PublicacionMapper::toResumen));
    }

    /**
     * Autos parecidos (D-06): hasta 4 por defecto, acotado a 1..8. Funciona para un auto en cualquier estado: el detalle
     * de un vendido sigue abierto por link directo y sugiere alternativas disponibles.
     */
    public List<PublicacionResumenResponse> similares(Long id, Integer limite) {
        Publicacion base = publicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + id));
        int tope = limite == null ? LIMITE_SIMILARES_POR_DEFECTO
                : Math.min(Math.max(limite, 1), LIMITE_SIMILARES_MAXIMO);
        // El mapeo va adentro de la transacción: agencia y fotos son lazy.
        return publicacionRepository.findAll(CatalogoSpecification.similaresA(base), PageRequest.of(0, tope))
                .stream()
                .map(PublicacionMapper::toResumen)
                .toList();
    }

    /**
     * Opciones y rangos de los filtros sobre el mismo conjunto visible del listado. No dependen de los filtros activos.
     * Se agrupa en Java: alcanza para cientos o pocos miles de autos; si el inventario crece conviene un GROUP BY.
     */
    public FacetasResponse facetas(Long agenciaId) {
        FiltrosCatalogo soloAgencia = new FiltrosCatalogo();
        soloAgencia.setAgenciaId(agenciaId);
        List<Publicacion> autos = publicacionRepository.findAll(
                Specification.where(CatalogoSpecification.visible(limiteVendidosVisibles()))
                        .and(CatalogoSpecification.conFiltros(soloAgencia)),
                Sort.by("id"));

        List<BigDecimal> precios = autos.stream().map(Publicacion::getPrecio).filter(Objects::nonNull).toList();
        RangoPrecio precio = precios.isEmpty() ? null : RangoPrecio.builder()
                .min(dosDecimales(Collections.min(precios)))
                .max(dosDecimales(Collections.max(precios)))
                .histograma(histograma(precios))
                .build();

        return FacetasResponse.builder()
                .marcas(contarTexto(autos, Publicacion::getMarca))
                .modelos(contarModelos(autos))
                .tipos(contarEnum(autos, Publicacion::getTipoCarroceria))
                .zonas(contarEnum(autos, p -> p.getAgencia().getZona()))
                .colores(contarTexto(autos, Publicacion::getColor))
                .transmisiones(contarEnum(autos, Publicacion::getTransmision))
                .estados(contarEnum(autos, Publicacion::getEstado))
                .anio(rango(autos, Publicacion::getAnio))
                .kilometraje(rango(autos, Publicacion::getKilometraje))
                .precio(precio)
                .build();
    }

    /**
     * Copia de los filtros con los parámetros raros neutralizados: página mínima 1, orden desconocido a relevancia,
     * hasta 20 valores por lista (sin vacíos), búsqueda de hasta 60 caracteres. No modifica la entrada.
     */
    static FiltrosCatalogo normalizar(FiltrosCatalogo crudos) {
        FiltrosCatalogo f = new FiltrosCatalogo();
        // Una página fuera de 1..MAX_PAGINA cae a la primera: un offset mayor que Integer.MAX_VALUE haría lanzar a
        // Spring Data (500 con stack trace en el log, en un endpoint sin token).
        Integer pagina = crudos.getPagina();
        f.setPagina(pagina == null || pagina < 1 || pagina > MAX_PAGINA ? 1 : pagina);
        f.setOrden(OrdenCatalogo.desde(crudos.getOrden()).name());
        String busqueda = crudos.getBusqueda() == null ? "" : crudos.getBusqueda().trim();
        if (busqueda.length() > MAX_LARGO_BUSQUEDA) {
            busqueda = busqueda.substring(0, MAX_LARGO_BUSQUEDA).trim();
        }
        f.setBusqueda(busqueda.isEmpty() ? null : busqueda);
        f.setMarca(limpiarTexto(crudos.getMarca()));
        f.setModelo(limpiarTexto(crudos.getModelo()));
        f.setColor(limpiarTexto(crudos.getColor()));
        f.setTransmision(acotar(crudos.getTransmision()));
        f.setTipo(acotar(crudos.getTipo()));
        f.setZona(acotar(crudos.getZona()));
        f.setEstado(acotar(crudos.getEstado()));
        f.setAnioMin(crudos.getAnioMin());
        f.setAnioMax(crudos.getAnioMax());
        f.setKmMax(crudos.getKmMax());
        f.setPrecioMin(crudos.getPrecioMin());
        f.setPrecioMax(crudos.getPrecioMax());
        f.setOfertas(crudos.getOfertas());
        f.setAgenciaId(crudos.getAgenciaId());
        return f;
    }

    private static List<String> limpiarTexto(List<String> valores) {
        if (valores == null) {
            return null;
        }
        return valores.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .limit(MAX_VALORES_POR_FILTRO)
                .toList();
    }

    private static <T> List<T> acotar(List<T> valores) {
        if (valores == null) {
            return null;
        }
        return valores.stream().filter(Objects::nonNull).limit(MAX_VALORES_POR_FILTRO).toList();
    }

    private LocalDateTime limiteVendidosVisibles() {
        return LocalDateTime.now(clock).minusDays(diasVendidoVisible);
    }

    // ---- Facetas ----

    // Agrupa sin distinguir mayúsculas (igual que el filtro) y muestra la primera grafía vista; orden alfabético.
    private static List<Conteo> contarTexto(List<Publicacion> autos, Function<Publicacion, String> campo) {
        Map<String, String> grafia = new HashMap<>();
        Map<String, Long> cantidad = new TreeMap<>();
        for (Publicacion p : autos) {
            String valor = campo.apply(p);
            if (valor == null || valor.isBlank()) {
                continue;
            }
            String clave = valor.trim().toLowerCase(Locale.ROOT);
            grafia.putIfAbsent(clave, valor.trim());
            cantidad.merge(clave, 1L, Long::sum);
        }
        return cantidad.entrySet().stream()
                .map(e -> Conteo.builder().valor(grafia.get(e.getKey())).cantidad(e.getValue()).build())
                .toList();
    }

    private static List<ConteoModelo> contarModelos(List<Publicacion> autos) {
        Map<String, String[]> grafia = new HashMap<>();
        Map<String, Long> cantidad = new TreeMap<>();
        for (Publicacion p : autos) {
            if (p.getMarca() == null || p.getModelo() == null || p.getModelo().isBlank()) {
                continue;
            }
            String marca = p.getMarca().trim();
            String modelo = p.getModelo().trim();
            String clave = marca.toLowerCase(Locale.ROOT) + "|" + modelo.toLowerCase(Locale.ROOT);
            grafia.putIfAbsent(clave, new String[]{marca, modelo});
            cantidad.merge(clave, 1L, Long::sum);
        }
        return cantidad.entrySet().stream()
                .map(e -> ConteoModelo.builder().marca(grafia.get(e.getKey())[0]).valor(grafia.get(e.getKey())[1])
                        .cantidad(e.getValue()).build())
                .toList();
    }

    // En el orden del enum, sin el valor null; el front pone las etiquetas.
    private static <E extends Enum<E>> List<Conteo> contarEnum(List<Publicacion> autos, Function<Publicacion, E> campo) {
        Map<E, Long> cantidad = new TreeMap<>();
        for (Publicacion p : autos) {
            E valor = campo.apply(p);
            if (valor != null) {
                cantidad.merge(valor, 1L, Long::sum);
            }
        }
        return cantidad.entrySet().stream()
                .map(e -> Conteo.builder().valor(e.getKey().name()).cantidad(e.getValue()).build())
                .toList();
    }

    private static Rango rango(List<Publicacion> autos, Function<Publicacion, Integer> campo) {
        List<Integer> valores = autos.stream().map(campo).filter(Objects::nonNull).toList();
        return Rango.builder()
                .min(valores.isEmpty() ? null : Collections.min(valores))
                .max(valores.isEmpty() ? null : Collections.max(valores))
                .build();
    }

    /**
     * 16 tramos de igual ancho entre el precio mínimo y el máximo: [desde, hasta) salvo el último, que incluye el
     * máximo. Si todos los precios son iguales hay un solo tramo. La suma de cantidades es siempre la cantidad de precios.
     */
    static List<TramoPrecio> histograma(List<BigDecimal> precios) {
        if (precios.isEmpty()) {
            return List.of();
        }
        BigDecimal min = Collections.min(precios);
        BigDecimal max = Collections.max(precios);
        BigDecimal rango = max.subtract(min);
        if (rango.signum() == 0) {
            return List.of(TramoPrecio.builder().desde(dosDecimales(min)).hasta(dosDecimales(max)).cantidad(precios.size()).build());
        }

        long[] cantidades = new long[CANTIDAD_TRAMOS_PRECIO];
        for (BigDecimal precio : precios) {
            // Piso exacto de (precio - min) * 16 / rango; el máximo cae en el último tramo.
            int indice = precio.subtract(min).multiply(BigDecimal.valueOf(CANTIDAD_TRAMOS_PRECIO))
                    .divide(rango, 0, RoundingMode.DOWN).intValue();
            cantidades[Math.min(indice, CANTIDAD_TRAMOS_PRECIO - 1)]++;
        }

        List<TramoPrecio> tramos = new ArrayList<>();
        for (int i = 0; i < CANTIDAD_TRAMOS_PRECIO; i++) {
            BigDecimal desde = min.add(limiteDeTramo(rango, i));
            BigDecimal hasta = i == CANTIDAD_TRAMOS_PRECIO - 1 ? max : min.add(limiteDeTramo(rango, i + 1));
            tramos.add(TramoPrecio.builder().desde(dosDecimales(desde)).hasta(dosDecimales(hasta)).cantidad(cantidades[i]).build());
        }
        return tramos;
    }

    private static BigDecimal limiteDeTramo(BigDecimal rango, int indice) {
        return rango.multiply(BigDecimal.valueOf(indice))
                .divide(BigDecimal.valueOf(CANTIDAD_TRAMOS_PRECIO), 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal dosDecimales(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
