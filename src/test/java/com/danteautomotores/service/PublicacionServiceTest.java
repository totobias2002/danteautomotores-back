package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.CambiarDestacadoRequest;
import com.danteautomotores.dto.publicacion.CambiarEstadoRequest;
import com.danteautomotores.dto.publicacion.PublicacionRequest;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.ReordenarFotosRequest;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.FotoPublicacion;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.FotoPublicacionRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private AgenciaRepository agenciaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FotoPublicacionRepository fotoPublicacionRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ImagenValidator imagenValidator;

    @InjectMocks
    private PublicacionService publicacionService;

    @BeforeEach
    void autenticarComoAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("admin@dante.com", null));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static PublicacionRequest requestValido(Long agenciaId) {
        PublicacionRequest request = new PublicacionRequest();
        request.setAgenciaId(agenciaId);
        request.setMarca("Toyota");
        request.setModelo("Corolla");
        request.setAnio(2020);
        request.setPrecio(new BigDecimal("15000"));
        return request;
    }

    private static Agencia agencia(Long id, String nombre) {
        return Agencia.builder().id(id).nombre(nombre).slug(nombre.toLowerCase()).build();
    }

    private void stubAdminAutenticado() {
        Usuario admin = Usuario.builder().id(1L).email("admin@dante.com").rol(Rol.ADMIN).build();
        when(usuarioRepository.findByEmail("admin@dante.com")).thenReturn(Optional.of(admin));
    }

    @Test
    void crearConUnaAgenciaExistenteGuardaLaPublicacionEnEsaAgencia() {
        stubAdminAutenticado();
        Agencia sucursal = agencia(2L, "Sucursal");
        when(agenciaRepository.findById(2L)).thenReturn(Optional.of(sucursal));

        publicacionService.crear(requestValido(2L));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getAgencia()).isSameAs(sucursal);
        assertThat(guardada.getValue().getMarca()).isEqualTo("Toyota");
    }

    @Test
    void actualizarConOtraAgenciaMueveLaPublicacionAEsaAgencia() {
        Agencia original = agencia(1L, "Dante");
        Agencia destino = agencia(2L, "Sucursal");
        Publicacion existente = Publicacion.builder().id(10L).agencia(original)
                .marca("Toyota").modelo("Corolla").build();
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(agenciaRepository.findById(2L)).thenReturn(Optional.of(destino));

        publicacionService.actualizar(10L, requestValido(2L));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getAgencia()).isSameAs(destino);
    }

    @Test
    void crearConUnaAgenciaInexistenteLanzaNotFoundYNoGuardaNada() {
        when(agenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicacionService.crear(requestValido(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe una agencia con id: 99");

        verify(publicacionRepository, never()).save(any());
    }

    private static CambiarDestacadoRequest destacadoRequest(boolean valor) {
        CambiarDestacadoRequest request = new CambiarDestacadoRequest();
        request.setDestacado(valor);
        return request;
    }

    private static Publicacion publicacionExistente(boolean destacado) {
        return Publicacion.builder().id(10L).agencia(agencia(1L, "Dante"))
                .marca("Toyota").modelo("Corolla").destacado(destacado).build();
    }

    @Test
    void cambiarDestacadoAVerdaderoGuardaYDevuelveElFlag() {
        Publicacion existente = publicacionExistente(false);
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(existente));

        PublicacionResponse respuesta = publicacionService.cambiarDestacado(10L, destacadoRequest(true));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().isDestacado()).isTrue();
        assertThat(respuesta.isDestacado()).isTrue();
    }

    @Test
    void cambiarDestacadoAFalsoLoDesmarca() {
        Publicacion existente = publicacionExistente(true);
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(existente));

        PublicacionResponse respuesta = publicacionService.cambiarDestacado(10L, destacadoRequest(false));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().isDestacado()).isFalse();
        assertThat(respuesta.isDestacado()).isFalse();
    }

    @Test
    void cambiarDestacadoDeUnIdInexistenteLanzaNotFound() {
        when(publicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicacionService.cambiarDestacado(99L, destacadoRequest(true)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void pasarUnAutoDestacadoAVendidoNoLoDesmarca() {
        Publicacion existente = publicacionExistente(true);
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(existente));
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setEstado(EstadoPublicacion.VENDIDO);

        PublicacionResponse respuesta = publicacionService.cambiarEstado(10L, request);

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoPublicacion.VENDIDO);
        assertThat(guardada.getValue().isDestacado()).isTrue();
        assertThat(respuesta.isDestacado()).isTrue();
    }

    @Test
    void actualizarUnaPublicacionDestacadaNoCambiaElDestacado() {
        Publicacion existente = publicacionExistente(true);
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(agenciaRepository.findById(1L)).thenReturn(Optional.of(existente.getAgencia()));

        publicacionService.actualizar(10L, requestValido(1L));

        ArgumentCaptor<Publicacion> guardada = ArgumentCaptor.forClass(Publicacion.class);
        verify(publicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().isDestacado()).isTrue();
    }

    @Test
    void unaPublicacionNuevaNaceSinDestacar() {
        assertThat(Publicacion.builder().build().isDestacado()).isFalse();
    }

    // ---- fotos: subida y reorden ----

    private static final CloudinaryService.ImagenSubida SUBIDA =
            new CloudinaryService.ImagenSubida("https://res.cloudinary.com/x.jpg", "danteautomotores/publicaciones/abc");

    private static MockMultipartFile archivoDeFoto() {
        return new MockMultipartFile("archivo", "auto.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private static FotoPublicacion foto(Long id, Integer orden) {
        return FotoPublicacion.builder().id(id).url("https://x/" + id + ".jpg").orden(orden).build();
    }

    private static Publicacion publicacionConFotos(FotoPublicacion... fotos) {
        Publicacion publicacion = publicacionExistente(false);
        for (FotoPublicacion f : fotos) {
            f.setPublicacion(publicacion);
            publicacion.getFotos().add(f);
        }
        return publicacion;
    }

    private static ReordenarFotosRequest orden(Long... ids) {
        ReordenarFotosRequest request = new ReordenarFotosRequest();
        request.setFotoIds(List.of(ids));
        return request;
    }

    @Test
    void agregarFotoEnUnAutoConDiezFotosFallaYNoSubeNada() {
        FotoPublicacion[] diez = new FotoPublicacion[ImagenValidator.MAX_FOTOS];
        for (int i = 0; i < diez.length; i++) {
            diez[i] = foto((long) i + 1, i);
        }
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacionConFotos(diez)));

        assertThatThrownBy(() -> publicacionService.agregarFoto(10L, archivoDeFoto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada auto puede tener hasta 10 fotos");

        verify(cloudinaryService, never()).subir(any());
        verify(fotoPublicacionRepository, never()).save(any());
    }

    @Test
    void agregarFotoNoSubeNiGuardaSiElValidadorRechazaElArchivo() {
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacionConFotos()));
        doThrow(new IllegalArgumentException("Formato no permitido. Usá JPG, PNG o WebP"))
                .when(imagenValidator).validar(any());

        assertThatThrownBy(() -> publicacionService.agregarFoto(10L, archivoDeFoto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato no permitido. Usá JPG, PNG o WebP");

        verify(cloudinaryService, never()).subir(any());
        verify(fotoPublicacionRepository, never()).save(any());
    }

    @Test
    void agregarFotoTomaElMaximoOrdenMasUnoAunqueSeHayaBorradoUnaDelMedio() {
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacionConFotos(foto(1L, 0), foto(3L, 2))));
        when(cloudinaryService.subir(any())).thenReturn(SUBIDA);

        publicacionService.agregarFoto(10L, archivoDeFoto());

        ArgumentCaptor<FotoPublicacion> guardada = ArgumentCaptor.forClass(FotoPublicacion.class);
        verify(fotoPublicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getOrden()).isEqualTo(3);
    }

    @Test
    void agregarFotoConFotosSinOrdenDejaLaPrimeraNuevaEnCero() {
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacionConFotos(foto(1L, null))));
        when(cloudinaryService.subir(any())).thenReturn(SUBIDA);

        publicacionService.agregarFoto(10L, archivoDeFoto());

        ArgumentCaptor<FotoPublicacion> guardada = ArgumentCaptor.forClass(FotoPublicacion.class);
        verify(fotoPublicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getOrden()).isZero();
    }

    @Test
    void agregarFotoGuardaLaUrlYElPublicIdQueDevuelveCloudinary() {
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacionConFotos()));
        when(cloudinaryService.subir(any())).thenReturn(SUBIDA);

        PublicacionResponse respuesta = publicacionService.agregarFoto(10L, archivoDeFoto());

        ArgumentCaptor<FotoPublicacion> guardada = ArgumentCaptor.forClass(FotoPublicacion.class);
        verify(fotoPublicacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getUrl()).isEqualTo("https://res.cloudinary.com/x.jpg");
        assertThat(guardada.getValue().getPublicId()).isEqualTo("danteautomotores/publicaciones/abc");
        assertThat(respuesta.getFotos()).hasSize(1);
    }

    @Test
    void reordenarFotosAsignaElOrdenSegunLaPosicionYLaPrimeraEsLaPortada() {
        Publicacion publicacion = publicacionConFotos(foto(1L, 0), foto(2L, 1), foto(3L, 2));
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacion));

        PublicacionResponse respuesta = publicacionService.reordenarFotos(10L, orden(3L, 1L, 2L));

        assertThat(respuesta.getFotos()).extracting(f -> f.getId()).containsExactly(3L, 1L, 2L);
        assertThat(respuesta.getFotos()).extracting(f -> f.getOrden()).containsExactly(0, 1, 2);
    }

    @Test
    void reordenarFotosConUnIdFaltanteFalla() {
        Publicacion publicacion = publicacionConFotos(foto(1L, 0), foto(2L, 1), foto(3L, 2));
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacion));

        assertThatThrownBy(() -> publicacionService.reordenarFotos(10L, orden(3L, 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El orden debe incluir todas las fotos del auto, una sola vez cada una");
        assertThat(publicacion.getFotos()).extracting(FotoPublicacion::getOrden).containsExactly(0, 1, 2);
    }

    @Test
    void reordenarFotosConUnDuplicadoFalla() {
        Publicacion publicacion = publicacionConFotos(foto(1L, 0), foto(2L, 1), foto(3L, 2));
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacion));

        assertThatThrownBy(() -> publicacionService.reordenarFotos(10L, orden(1L, 1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El orden debe incluir todas las fotos del auto, una sola vez cada una");
    }

    @Test
    void reordenarFotosConUnIdDeOtraPublicacionFalla() {
        Publicacion publicacion = publicacionConFotos(foto(1L, 0), foto(2L, 1), foto(3L, 2));
        when(publicacionRepository.findById(10L)).thenReturn(Optional.of(publicacion));

        assertThatThrownBy(() -> publicacionService.reordenarFotos(10L, orden(1L, 2L, 99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El orden debe incluir todas las fotos del auto, una sola vez cada una");
    }

    @Test
    void reordenarFotosDeUnaPublicacionInexistenteLanzaNotFound() {
        when(publicacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicacionService.reordenarFotos(99L, orden(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
