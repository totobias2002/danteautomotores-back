package com.danteautomotores.service;

import com.danteautomotores.dto.consulta.ConsultaRequest;
import com.danteautomotores.dto.consulta.ConsultaResponse;
import com.danteautomotores.entity.Consulta;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.ConsultaMapper;
import com.danteautomotores.repository.ConsultaRepository;
import com.danteautomotores.repository.PublicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final PublicacionRepository publicacionRepository;

    public ConsultaResponse crear(ConsultaRequest request) {
        Publicacion publicacion = publicacionRepository.findById(request.getPublicacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + request.getPublicacionId()));

        Consulta consulta = Consulta.builder()
                .publicacion(publicacion)
                .nombreComprador(request.getNombreComprador())
                .emailComprador(request.getEmailComprador())
                .telefonoComprador(request.getTelefonoComprador())
                .mensaje(request.getMensaje())
                .build();

        consultaRepository.save(consulta);
        return ConsultaMapper.toResponse(consulta);
    }

    public List<ConsultaResponse> listarPorPublicacion(Long publicacionId) {
        return consultaRepository.findByPublicacionIdOrderByFechaDesc(publicacionId).stream()
                .map(ConsultaMapper::toResponse)
                .toList();
    }
}
