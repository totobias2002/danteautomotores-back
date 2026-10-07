package com.danteautomotores.service;

import com.danteautomotores.dto.usuario.UsuarioFichaResponse;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.DatoFaltante;
import com.danteautomotores.enums.Rol;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.repository.ConversacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lo que la agencia sabe de un usuario (MSG-09). Solo se llega desde /api/admin/**. Es el único lugar donde el DNI y
 * el teléfono de una persona salen hacia otra (Ley 25.326): por eso no registra nada en el log (T-04-32).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final ConversacionRepository conversacionRepository;
    private final ConversacionAdminService conversacionAdminService;
    private final VerificacionCuenta verificacionCuenta;

    /**
     * La ficha de un comprador, con su historial de conversaciones (la de mensaje más reciente primero). Una cuenta
     * admin responde igual que una inexistente: el mismo 404 con el mismo mensaje (T-04-31).
     */
    @Transactional(readOnly = true)
    public UsuarioFichaResponse obtenerFicha(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getRol() == Rol.COMPRADOR)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario"));
        List<DatoFaltante> faltantes = verificacionCuenta.faltantes(usuario);
        return UsuarioFichaResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .dni(usuario.getDni())
                .emailConfirmado(usuario.isEmailConfirmado())
                .cuentaVerificada(faltantes.isEmpty())
                .faltantes(faltantes)
                .fechaRegistro(usuario.getFechaRegistro() == null ? null : usuario.getFechaRegistro().toLocalDate())
                .conversaciones(conversacionAdminService.resumir(
                        conversacionRepository.findByUsuarioIdOrderByUltimoMensajeEnDescIdDesc(usuario.getId())))
                .build();
    }
}
