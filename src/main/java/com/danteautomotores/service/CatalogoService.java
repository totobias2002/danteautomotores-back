package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.FiltrosCatalogo;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.dto.publicacion.PublicacionResumenResponse;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.OrdenCatalogo;
import com.danteautomotores.mapper.PublicacionMapper;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.spec.CatalogoSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

    // Tamaño de página fijo (D-07): el cliente no lo elige.
    static final int TAMANIO_PAGINA = 24;
    static final int MAX_VALORES_POR_FILTRO = 20;
    static final int MAX_LARGO_BUSQUEDA = 60;

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
     * Copia de los filtros con los parámetros raros neutralizados: página mínima 1, orden desconocido a relevancia,
     * hasta 20 valores por lista (sin vacíos), búsqueda de hasta 60 caracteres. No modifica la entrada.
     */
    static FiltrosCatalogo normalizar(FiltrosCatalogo crudos) {
        FiltrosCatalogo f = new FiltrosCatalogo();
        f.setPagina(crudos.getPagina() == null || crudos.getPagina() < 1 ? 1 : crudos.getPagina());
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
}
