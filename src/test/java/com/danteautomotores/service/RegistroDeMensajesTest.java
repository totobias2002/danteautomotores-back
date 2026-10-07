package com.danteautomotores.service;

import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.Conversacion;
import com.danteautomotores.entity.Mensaje;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.AutorMensaje;
import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoConversacion;
import com.danteautomotores.repository.MensajeRepository;
import com.danteautomotores.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroDeMensajesTest {

    private static final String TEXTO = "TEXTO-SECRETO-DEL-MENSAJE-77c1";

    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacionesService notificaciones;

    private RegistroDeMensajes registro;
    private Usuario ana;
    private Usuario admin;

    @BeforeEach
    void armar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-07T15:30:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        registro = new RegistroDeMensajes(mensajeRepository, reloj, usuarioRepository, notificaciones);
        ana = Usuario.builder().id(3L).nombre("Ana").apellido("Pérez").email("ana@cuenta.com")
                .rol(Rol.COMPRADOR).build();
        admin = Usuario.builder().id(1L).nombre("Admin").email("admin@dante.test").rol(Rol.ADMIN).build();
    }

    @AfterEach
    void limpiarSincronizaciones() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Conversacion conversacion(Publicacion auto) {
        LocalDateTime t = LocalDateTime.of(2026, 10, 7, 12, 0);
        return Conversacion.builder().id(42L).tipo(auto == null ? TipoConversacion.COTIZACION : TipoConversacion.COMPRA)
                .estado(EstadoConversacion.ABIERTA).usuario(ana).publicacion(auto).creadaEn(t).ultimoMensajeEn(t).build();
    }

    private Publicacion corolla() {
        return Publicacion.builder().id(7L).marca("Toyota").modelo("Corolla").anio(2020)
                .agencia(Agencia.builder().id(1L).nombre("Dante").slug("dante").build()).build();
    }

    private void guardaDevuelveElMensaje() {
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void confirmarLaTransaccion() {
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCommit();
        }
    }

    @Test
    void unMensajeDelUsuarioAvisaACadaAdminSoloDespuesDelCommit() {
        guardaDevuelveElMensaje();
        Usuario otroAdmin = Usuario.builder().id(2L).nombre("Otro").email("otro@dante.test").rol(Rol.ADMIN).build();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin, otroAdmin));
        TransactionSynchronizationManager.initSynchronization();

        registro.agregar(conversacion(corolla()), ana, AutorMensaje.USUARIO, TEXTO);

        verifyNoInteractions(notificaciones);

        confirmarLaTransaccion();

        verify(notificaciones).enviarAvisoDeMensajeALaAgencia("admin@dante.test", "Admin", "Ana Pérez",
                "Toyota Corolla 2020", 42L);
        verify(notificaciones).enviarAvisoDeMensajeALaAgencia("otro@dante.test", "Otro", "Ana Pérez",
                "Toyota Corolla 2020", 42L);
        verify(notificaciones, times(2)).enviarAvisoDeMensajeALaAgencia(anyString(), anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    void unMensajeDeLaAgenciaAvisaUnaVezAlDuenoDeLaConversacion() {
        guardaDevuelveElMensaje();
        TransactionSynchronizationManager.initSynchronization();

        registro.agregar(conversacion(corolla()), admin, AutorMensaje.AGENCIA, TEXTO);

        verifyNoInteractions(notificaciones);

        confirmarLaTransaccion();

        verify(notificaciones, times(1)).enviarAvisoDeMensajeAlUsuario("ana@cuenta.com", "Ana",
                "Toyota Corolla 2020", 42L);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void siLaTransaccionSeRevierteNoSeAvisaANadie() {
        guardaDevuelveElMensaje();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin));
        TransactionSynchronizationManager.initSynchronization();

        registro.agregar(conversacion(corolla()), ana, AutorMensaje.USUARIO, TEXTO);
        registro.agregar(conversacion(corolla()), admin, AutorMensaje.AGENCIA, TEXTO);
        // Nunca se ejecuta afterCommit: se revierte.
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(notificaciones);
    }

    @Test
    void sinTransaccionActivaElAvisoSaleEnseguida() {
        guardaDevuelveElMensaje();

        registro.agregar(conversacion(corolla()), admin, AutorMensaje.AGENCIA, TEXTO);

        verify(notificaciones).enviarAvisoDeMensajeAlUsuario("ana@cuenta.com", "Ana", "Toyota Corolla 2020", 42L);
    }

    @Test
    void sinCuentasAdminNoSeAvisaNada() {
        guardaDevuelveElMensaje();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of());
        TransactionSynchronizationManager.initSynchronization();

        registro.agregar(conversacion(corolla()), ana, AutorMensaje.USUARIO, TEXTO);
        confirmarLaTransaccion();

        verifyNoInteractions(notificaciones);
    }

    @Test
    void unaConversacionSinAutoUsaUnaCotizacion() {
        guardaDevuelveElMensaje();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin));
        TransactionSynchronizationManager.initSynchronization();

        registro.agregar(conversacion(null), ana, AutorMensaje.USUARIO, TEXTO);
        confirmarLaTransaccion();

        verify(notificaciones).enviarAvisoDeMensajeALaAgencia("admin@dante.test", "Admin", "Ana Pérez",
                "una cotización", 42L);
    }

    @Test
    void elTextoDelMensajeNuncaSePasaAlAviso() {
        guardaDevuelveElMensaje();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin));
        TransactionSynchronizationManager.initSynchronization();

        registro.agregar(conversacion(corolla()), ana, AutorMensaje.USUARIO, TEXTO);
        confirmarLaTransaccion();

        verify(notificaciones).enviarAvisoDeMensajeALaAgencia(eq("admin@dante.test"), eq("Admin"), eq("Ana Pérez"),
                eq("Toyota Corolla 2020"), eq(42L));
    }

    @Test
    void siElEncoladoFallaElMensajeIgualSeDevuelveYNoSePropaga() {
        guardaDevuelveElMensaje();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin));
        doThrow(new IllegalStateException("cola llena")).when(notificaciones)
                .enviarAvisoDeMensajeALaAgencia(anyString(), anyString(), anyString(), anyString(), anyLong());
        TransactionSynchronizationManager.initSynchronization();

        Mensaje mensaje = registro.agregar(conversacion(corolla()), ana, AutorMensaje.USUARIO, TEXTO);

        assertThat(mensaje.getTexto()).isEqualTo(TEXTO);
        assertThatCode(this::confirmarLaTransaccion).doesNotThrowAnyException();
    }

    @Test
    void siFallaLaBusquedaDeAdminsElMensajeIgualSeGuarda() {
        guardaDevuelveElMensaje();
        when(usuarioRepository.findByRol(Rol.ADMIN)).thenThrow(new IllegalStateException("base caída"));
        TransactionSynchronizationManager.initSynchronization();

        Conversacion conversacion = conversacion(corolla());
        Mensaje mensaje = registro.agregar(conversacion, ana, AutorMensaje.USUARIO, TEXTO);

        assertThat(mensaje).isNotNull();
        assertThat(conversacion.getUltimoMensajeEn()).isEqualTo(LocalDateTime.of(2026, 10, 7, 15, 30));
        confirmarLaTransaccion();
        verifyNoInteractions(notificaciones);
    }
}
