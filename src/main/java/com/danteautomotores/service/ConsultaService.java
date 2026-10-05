package com.danteautomotores.service;

import com.danteautomotores.dto.consulta.ConsultaRequest;
import com.danteautomotores.dto.consulta.ConsultaResponse;
import com.danteautomotores.entity.Consulta;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.ConsultaMapper;
import com.danteautomotores.repository.ConsultaRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final VerificacionCuenta verificacionCuenta;

    public ConsultaResponse crear(ConsultaRequest request, String email) {
        // La autorización se decide primero y con el estado actual de la base (no con un claim del token):
        // así no se revela si la publicación existe a quien no puede consultar.
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        verificacionCuenta.exigir(usuario);

        Publicacion publicacion = publicacionRepository.findById(request.getPublicacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + request.getPublicacionId()));

        // Defensa en profundidad (D-06): el front oculta el formulario de un vendido, pero el endpoint es público.
        // Un RESERVADO sí se consulta (D-05): si la reserva se cae, el auto vuelve a estar disponible.
        if (publicacion.getEstado() == EstadoPublicacion.VENDIDO) {
            throw new ReglaDeNegocioException("Este auto ya se vendió");
        }

        // Foto de los datos de la cuenta al momento de la consulta (D-11): la tabla no cambia.
        Consulta consulta = Consulta.builder()
                .publicacion(publicacion)
                .nombreComprador(usuario.getNombre() + " " + usuario.getApellido())
                .emailComprador(usuario.getEmail())
                .telefonoComprador(usuario.getTelefono())
                .mensaje(request.getMensaje())
                .build();

        consultaRepository.save(consulta);
        return ConsultaMapper.toResponse(consulta);
    }

    @Transactional(readOnly = true)
    public List<ConsultaResponse> listarPorPublicacion(Long publicacionId) {
        return consultaRepository.findByPublicacionIdOrderByFechaDesc(publicacionId).stream()
                .map(ConsultaMapper::toResponse)
                .toList();
    }
}
