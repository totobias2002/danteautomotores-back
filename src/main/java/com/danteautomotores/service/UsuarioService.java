package com.danteautomotores.service;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.usuario.ActualizarPerfilRequest;
import com.danteautomotores.dto.usuario.CambiarContrasenaRequest;
import com.danteautomotores.dto.usuario.UsuarioResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.enums.TipoTokenCuenta;
import com.danteautomotores.exception.LimiteDeIntentosException;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.UsuarioMapper;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.service.identidad.NormalizadorDeContacto;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Perfil de la propia cuenta: ver y completar o editar los datos de identidad. Nunca recibe un id: la cuenta sale del
 * mail autenticado. Por la Ley 25.326 no se loguea ningún dato personal (DNI, teléfono, mail).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService {

    public static final String MENSAJE_DNI_DUPLICADO = "Ese DNI ya está registrado. Si es tuyo, recuperá tu contraseña.";
    public static final String MENSAJE_DNI_INMUTABLE = "El DNI no se puede modificar desde la web. Escribinos si hay un error.";
    public static final String MENSAJE_DNI_OBLIGATORIO = "Ingresá tu DNI.";

    public static final String MENSAJE_CUENTA_DE_GOOGLE =
            "Tu cuenta ingresa con Google: definí una contraseña desde \"Olvidé mi contraseña\".";
    public static final String MENSAJE_CONTRASENA_ACTUAL_INCORRECTA = "La contraseña actual no es correcta.";
    public static final String MENSAJE_REENVIO_ENVIADO =
            "Te mandamos un mail para confirmar tu cuenta. Revisá también la carpeta de spam.";
    public static final String MENSAJE_MAIL_YA_CONFIRMADO = "Tu mail ya está confirmado.";

    private static final String RESTRICCION_DNI_UNICO = "uk_usuarios_dni";

    // Política de límites (ajustable): contraseña actual incorrecta 5 por cuenta cada 15 minutos; reenvío 3 por hora.
    private static final int MAX_FALLOS_CONTRASENA_ACTUAL = 5;
    private static final Duration VENTANA_CONTRASENA_ACTUAL = Duration.ofMinutes(15);
    private static final int MAX_REENVIOS = 3;
    private static final Duration VENTANA_REENVIOS = Duration.ofHours(1);

    private final UsuarioRepository usuarioRepository;
    private final VerificacionCuenta verificacionCuenta;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final TokenCuentaService tokenCuentaService;
    private final NotificacionesService notificacionesService;
    private final LimitadorDeIntentos limitador;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPerfil(String email) {
        return construirRespuesta(buscar(email));
    }

    public UsuarioResponse actualizarPerfil(String email, ActualizarPerfilRequest request) {
        Usuario usuario = buscar(email);

        // Se valida todo antes de tocar la cuenta: un rechazo no deja cambios a medias.
        String telefono = NormalizadorDeContacto.normalizarCelular(request.getTelefono());
        String dniNuevo = resolverDni(usuario, request.getDni());

        usuario.setNombre(request.getNombre().trim());
        usuario.setApellido(request.getApellido().trim());
        usuario.setTelefono(telefono);
        if (dniNuevo != null) {
            usuario.setDni(dniNuevo);
        }

        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // El chequeo previo no es atómico: ante dos pedidos simultáneos con el mismo DNI manda el UNIQUE de la base.
            if (violaElDniUnico(e)) {
                throw new ReglaDeNegocioException(MENSAJE_DNI_DUPLICADO);
            }
            throw e;
        }
        return construirRespuesta(usuario);
    }

    /**
     * Cambia la contraseña exigiendo la actual (AUTH-05). Devuelve una sesión nueva: su token lleva el instante del
     * cambio, así la sesión actual sigue y todas las demás caen (D-19). Una cuenta solo-Google no tiene contraseña
     * que cambiar: se la manda a "Olvidé mi contraseña" para definir una.
     */
    public AuthResponse cambiarContrasena(String email, CambiarContrasenaRequest request) {
        Usuario usuario = buscar(email);
        if (usuario.getPasswordHash() == null) {
            throw new ReglaDeNegocioException(MENSAJE_CUENTA_DE_GOOGLE);
        }

        // Una sesión robada no puede adivinar la contraseña actual a fuerza bruta.
        String claveFallos = "cambiar-clave:" + usuario.getId();
        if (limitador.bloqueado(claveFallos, MAX_FALLOS_CONTRASENA_ACTUAL, VENTANA_CONTRASENA_ACTUAL)) {
            throw new LimiteDeIntentosException("Hiciste demasiados intentos. Esperá unos minutos y volvé a probar.",
                    VENTANA_CONTRASENA_ACTUAL.toSeconds());
        }
        if (!passwordEncoder.matches(request.getActual(), usuario.getPasswordHash())) {
            limitador.registrarFallo(claveFallos, VENTANA_CONTRASENA_ACTUAL);
            throw new ReglaDeNegocioException(MENSAJE_CONTRASENA_ACTUAL_INCORRECTA);
        }
        ContrasenaCuenta.verificarLargo(request.getNueva());

        usuario.setPasswordHash(passwordEncoder.encode(request.getNueva()));
        ContrasenaCuenta.marcarCambio(usuario, clock);
        usuarioRepository.save(usuario);
        tokenCuentaService.descartarPendientes(usuario.getId());
        limitador.olvidar(claveFallos, VENTANA_CONTRASENA_ACTUAL);

        // El aviso sale cuando el cambio ya está confirmado en la base.
        ContrasenaCuenta.despuesDelCommit(() -> notificacionesService.enviarContrasenaCambiada(usuario));
        return authService.iniciarSesion(usuario);
    }

    /**
     * Reenvía el mail de confirmación (D-21). No se manda mail masivo a las cuentas anteriores a la fase: sale cuando el
     * usuario toca el botón. Una cuenta ya confirmada recibe el aviso sin que se envíe nada.
     */
    public String reenviarConfirmacion(String email) {
        Usuario usuario = buscar(email);
        if (usuario.isEmailConfirmado()) {
            return MENSAJE_MAIL_YA_CONFIRMADO;
        }
        if (!limitador.intentar("reenvio:" + usuario.getId(), MAX_REENVIOS, VENTANA_REENVIOS)) {
            throw new LimiteDeIntentosException("Ya te mandamos varios mails. Esperá un rato antes de pedir otro.",
                    VENTANA_REENVIOS.toSeconds());
        }
        String token = tokenCuentaService.emitir(usuario.getId(), TipoTokenCuenta.CONFIRMAR_EMAIL);
        // El mail sale cuando el token ya está confirmado en la base.
        ContrasenaCuenta.despuesDelCommit(() -> notificacionesService.enviarConfirmacionEmail(usuario, token));
        return MENSAJE_REENVIO_ENVIADO;
    }

    /** Devuelve el DNI normalizado a guardar, o null si el DNI de la cuenta no cambia. */
    private String resolverDni(Usuario usuario, String dniPedido) {
        boolean pedidoConDni = dniPedido != null && !dniPedido.isBlank();

        if (tieneDni(usuario)) {
            if (pedidoConDni && !NormalizadorDeContacto.normalizarDni(dniPedido).equals(usuario.getDni())) {
                throw new ReglaDeNegocioException(MENSAJE_DNI_INMUTABLE);
            }
            return null;
        }
        if (!pedidoConDni) {
            // Un admin nunca queda obligado a tener DNI: no compra ni cotiza.
            if (usuario.getRol() == Rol.ADMIN) {
                return null;
            }
            throw new ReglaDeNegocioException(MENSAJE_DNI_OBLIGATORIO);
        }
        String dni = NormalizadorDeContacto.normalizarDni(dniPedido);
        if (usuarioRepository.existsByDniAndIdNot(dni, usuario.getId())) {
            throw new ReglaDeNegocioException(MENSAJE_DNI_DUPLICADO);
        }
        return dni;
    }

    private static boolean tieneDni(Usuario usuario) {
        return usuario.getDni() != null && !usuario.getDni().isBlank();
    }

    private static boolean violaElDniUnico(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion
                    && RESTRICCION_DNI_UNICO.equalsIgnoreCase(violacion.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private Usuario buscar(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta."));
    }

    private UsuarioResponse construirRespuesta(Usuario usuario) {
        return UsuarioMapper.toResponse(usuario, verificacionCuenta.faltantes(usuario));
    }
}
