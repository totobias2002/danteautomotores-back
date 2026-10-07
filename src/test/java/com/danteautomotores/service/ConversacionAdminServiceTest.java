package com.danteautomotores.service;

import com.danteautomotores.dto.conversacion.ConversacionResumenResponse;
import com.danteautomotores.dto.publicacion.PaginaResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.MensajeRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class ConversacionAdminServiceTest {

    private static final LocalDateTime AHORA_UTC = LocalDateTime.of(2026, 10, 7, 15, 30);

    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;

    private ConversacionAdminService servicio;

    @BeforeEach
    void armarElServicio() {
        servicio = new ConversacionAdminService(conversacionRepository, mensajeRepository);
    }

    private Conversacion conversacion(Long id, String nombre, String apellido) {
        Usuario usuario = Usuario.builder().id(id * 10).nombre(nombre).apellido(apellido)
                .email(nombre.toLowerCase() + "@x.com").dni("30111222").telefono("+5491100000000")
                .rol(Rol.COMPRADOR).build();
        Agencia agencia = Agencia.builder().id(1L).nombre("Agencia").slug("agencia").build();
        Publicacion auto = Publicacion.builder().id(5L).agencia(agencia).marca("Toyota").modelo("Corolla").anio(2020)
                .precio(new BigDecimal("1000000")).build();
        return Conversacion.builder().id(id).tipo(TipoConversacion.COMPRA).estado(EstadoConversacion.ABIERTA)
                .usuario(usuario).publicacion(auto).creadaEn(AHORA_UTC).ultimoMensajeEn(AHORA_UTC).build();
    }

    private Mensaje mensaje(Conversacion conversacion, String texto) {
        return Mensaje.builder().id(conversacion.getId() * 100).conversacion(conversacion).autorTipo(AutorMensaje.USUARIO)
                .texto(texto).creadoEn(AHORA_UTC).build();
    }

    private MensajeRepository.ConteoPorConversacion conteo(Long conversacionId, Long cantidad) {
        return new MensajeRepository.ConteoPorConversacion() {
            @Override
            public Long getConversacionId() {
                return conversacionId;
            }

            @Override
            public Long getCantidad() {
                return cantidad;
            }
        };
    }

    private Pageable pedirPagina(int pagina) {
        when(conversacionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        servicio.listar(null, null, false, pagina);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(conversacionRepository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    @Test
    void pideLaPaginaDeVeinteOrdenadaPorUltimoMensajeDescendenteYLuegoIdDescendente() {
        Pageable pageable = pedirPagina(3);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("ultimoMensajeEn").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().stream().map(Sort.Order::getProperty).toList()).containsExactly("ultimoMensajeEn", "id");
    }

    @Test
    void unaPaginaMenorA1SeTrataComoLaPrimera() {
        assertThat(pedirPagina(0).getPageNumber()).isZero();
    }

    @Test
    void unaPaginaNegativaSeTrataComoLaPrimera() {
        assertThat(pedirPagina(-4).getPageNumber()).isZero();
    }

    @Test
    void laSpecificationLlevaLosFiltrosRecibidos() {
        when(conversacionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        servicio.listar(TipoConversacion.COMPRA, EstadoConversacion.CERRADA, false, 1);

        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(conversacionRepository).findAll(captor.capture(), any(Pageable.class));

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path tipo = mock(Path.class);
        Path estado = mock(Path.class);
        when(root.get("tipo")).thenReturn(tipo);
        when(root.get("estado")).thenReturn(estado);
        when(cb.equal(any(), any(Object.class))).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        captor.getValue().toPredicate(root, query, cb);

        verify(cb).equal(tipo, TipoConversacion.COMPRA);
        verify(cb).equal(estado, EstadoConversacion.CERRADA);
        verify(query, never()).subquery(any());
    }

    @Test
    void sinFiltrosLaSpecificationNoAgregaPredicados() {
        when(conversacionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        servicio.listar(null, null, false, 1);

        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(conversacionRepository).findAll(captor.capture(), any(Pageable.class));
        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        ArgumentCaptor<Predicate[]> predicados = ArgumentCaptor.forClass(Predicate[].class);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        captor.getValue().toPredicate(root, query, cb);

        verify(cb).and(predicados.capture());
        assertThat(predicados.getValue()).isEmpty();
        verify(root, never()).get(any(String.class));
    }

    @Test
    void soloNoLeidasAgregaUnExistsSobreLosMensajesDelUsuarioSinLeer() {
        when(conversacionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        servicio.listar(null, null, true, 1);

        ArgumentCaptor<Specification> captor = ArgumentCaptor.forClass(Specification.class);
        verify(conversacionRepository).findAll(captor.capture(), any(Pageable.class));
        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Subquery sub = mock(Subquery.class);
        Root mensajeRoot = mock(Root.class);
        Path autorTipo = mock(Path.class);
        when(query.subquery(Long.class)).thenReturn(sub);
        when(sub.from(Mensaje.class)).thenReturn(mensajeRoot);
        when(mensajeRoot.get("autorTipo")).thenReturn(autorTipo);
        when(mensajeRoot.get("id")).thenReturn(mock(Path.class));
        when(mensajeRoot.get("conversacion")).thenReturn(mock(Path.class));
        when(mensajeRoot.get("leidoEn")).thenReturn(mock(Path.class));
        when(sub.select(any())).thenReturn(sub);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        captor.getValue().toPredicate(root, query, cb);

        verify(cb).equal(autorTipo, AutorMensaje.USUARIO);
        verify(cb).isNull(any());
        verify(cb).exists(sub);
    }

    @Test
    void armaElUsuarioYLosNoLeidosDelLadoDeLaAgenciaEnCadaFila() {
        Conversacion primera = conversacion(1L, "Ana", "Lopez");
        Conversacion segunda = conversacion(2L, "Beto", "Paz");
        when(conversacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(primera, segunda), PageRequest.of(0, 20), 2));
        when(mensajeRepository.findUltimosPorConversaciones(anyCollection()))
                .thenReturn(List.of(mensaje(primera, "Hola, quiero el auto"), mensaje(segunda, "Sigue disponible?")));
        when(mensajeRepository.contarNoLeidosPorConversacion(anyCollection(), eq(AutorMensaje.USUARIO)))
                .thenReturn(List.of(conteo(1L, 3L)));

        PaginaResponse<ConversacionResumenResponse> pagina = servicio.listar(null, null, false, 1);

        assertThat(pagina.contenido()).hasSize(2);
        ConversacionResumenResponse fila = pagina.contenido().get(0);
        assertThat(fila.getUsuario().getNombre()).isEqualTo("Ana");
        assertThat(fila.getUsuario().getApellido()).isEqualTo("Lopez");
        assertThat(fila.getUsuario().getEmail()).isEqualTo("ana@x.com");
        assertThat(fila.getUsuario().getId()).isEqualTo(10L);
        assertThat(fila.getNoLeidos()).isEqualTo(3L);
        assertThat(fila.getUltimoMensaje()).isEqualTo("Hola, quiero el auto");
        assertThat(fila.getPublicacion().getModelo()).isEqualTo("Corolla");
        ConversacionResumenResponse otra = pagina.contenido().get(1);
        assertThat(otra.getUsuario().getNombre()).isEqualTo("Beto");
        assertThat(otra.getNoLeidos()).isZero();
    }

    @Test
    void completaUltimoMensajeYNoLeidosConUnaConsultaPorPaginaNoUnaPorFila() {
        Conversacion primera = conversacion(1L, "Ana", "Lopez");
        Conversacion segunda = conversacion(2L, "Beto", "Paz");
        when(conversacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(primera, segunda), PageRequest.of(0, 20), 2));
        when(mensajeRepository.findUltimosPorConversaciones(anyCollection())).thenReturn(List.of());
        when(mensajeRepository.contarNoLeidosPorConversacion(anyCollection(), eq(AutorMensaje.USUARIO))).thenReturn(List.of());

        servicio.listar(null, null, false, 1);

        verify(mensajeRepository).findUltimosPorConversaciones(List.of(1L, 2L));
        verify(mensajeRepository).contarNoLeidosPorConversacion(List.of(1L, 2L), AutorMensaje.USUARIO);
    }

    @Test
    void unaPaginaVaciaNoConsultaMensajesYDevuelveLosTotales() {
        when(conversacionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PaginaResponse<ConversacionResumenResponse> pagina = servicio.listar(null, null, false, 1);

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamanio()).isEqualTo(20);
        assertThat(pagina.totalElementos()).isZero();
        verify(mensajeRepository, never()).findUltimosPorConversaciones(anyCollection());
        verify(mensajeRepository, never()).contarNoLeidosPorConversacion(anyCollection(), any());
    }
}
