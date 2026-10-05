package com.danteautomotores.service;

import com.danteautomotores.dto.solicitudventa.CambiarEstadoSolicitudRequest;
import com.danteautomotores.dto.solicitudventa.SolicitudVentaRequest;
import com.danteautomotores.dto.solicitudventa.SolicitudVentaResponse;
import com.danteautomotores.entity.SolicitudVenta;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.EstadoSolicitudVenta;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.SolicitudVentaMapper;
import com.danteautomotores.repository.SolicitudVentaRepository;
import com.danteautomotores.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudVentaService {

    private final SolicitudVentaRepository solicitudVentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VerificacionCuenta verificacionCuenta;

    public SolicitudVentaResponse crear(SolicitudVentaRequest request, String email) {
        // Cotizar exige cuenta verificada (D-01), decidido con el estado actual de la base. El contacto de la venta
        // (nombreVendedor, telefonoVendedor) sigue saliendo del formulario: puede diferir de los datos de la cuenta.
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta"));
        verificacionCuenta.exigir(usuario);

        SolicitudVenta solicitud = SolicitudVenta.builder()
                .marca(request.getMarca())
                .modelo(request.getModelo())
                .anio(request.getAnio())
                .kilometraje(request.getKilometraje())
                .cotizacionMin(request.getCotizacionMin())
                .cotizacionMax(request.getCotizacionMax())
                .nombreVendedor(request.getNombreVendedor())
                .telefonoVendedor(request.getTelefonoVendedor())
                .ciudad(request.getCiudad())
                .descripcion(request.getDescripcion())
                .estado(EstadoSolicitudVenta.PENDIENTE)
                .build();

        solicitudVentaRepository.save(solicitud);
        return SolicitudVentaMapper.toResponse(solicitud);
    }

    public List<SolicitudVentaResponse> listarTodas() {
        return solicitudVentaRepository.findAllByOrderByFechaDesc().stream()
                .map(SolicitudVentaMapper::toResponse)
                .toList();
    }

    public SolicitudVentaResponse cambiarEstado(Long id, CambiarEstadoSolicitudRequest request) {
        SolicitudVenta solicitud = solicitudVentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una solicitud de venta con id: " + id));
        solicitud.setEstado(request.getEstado());
        solicitudVentaRepository.save(solicitud);
        return SolicitudVentaMapper.toResponse(solicitud);
    }
}
