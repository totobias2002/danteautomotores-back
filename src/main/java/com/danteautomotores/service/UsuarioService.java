package com.danteautomotores.service;

import com.danteautomotores.dto.usuario.ActualizarPerfilRequest;
import com.danteautomotores.dto.usuario.UsuarioResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.UsuarioMapper;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.service.identidad.NormalizadorDeContacto;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String RESTRICCION_DNI_UNICO = "uk_usuarios_dni";

    private final UsuarioRepository usuarioRepository;
    private final VerificacionCuenta verificacionCuenta;

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
