package com.danteautomotores.service;

import com.danteautomotores.dto.agencia.AgenciaRequest;
import com.danteautomotores.dto.agencia.AgenciaResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.AgenciaMapper;
import com.danteautomotores.repository.AgenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenciaService {

    private final AgenciaRepository agenciaRepository;

    public List<AgenciaResponse> listar() {
        return agenciaRepository.findAll().stream()
                .map(AgenciaMapper::toResponse)
                .toList();
    }

    public AgenciaResponse obtenerPorSlug(String slug) {
        Agencia agencia = agenciaRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una agencia con slug: " + slug));
        return AgenciaMapper.toResponse(agencia);
    }

    public AgenciaResponse crear(AgenciaRequest request) {
        Agencia agencia = Agencia.builder()
                .nombre(request.getNombre())
                .slug(generarSlugUnico(request.getNombre()))
                .logo(request.getLogo())
                .descripcion(request.getDescripcion())
                .direccion(request.getDireccion())
                .telefonoContacto(request.getTelefonoContacto())
                .emailContacto(request.getEmailContacto())
                .build();

        agenciaRepository.save(agencia);
        return AgenciaMapper.toResponse(agencia);
    }

    public AgenciaResponse actualizar(Long id, AgenciaRequest request) {
        Agencia agencia = agenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una agencia con id: " + id));

        agencia.setNombre(request.getNombre());
        agencia.setLogo(request.getLogo());
        agencia.setDescripcion(request.getDescripcion());
        agencia.setDireccion(request.getDireccion());
        agencia.setTelefonoContacto(request.getTelefonoContacto());
        agencia.setEmailContacto(request.getEmailContacto());

        agenciaRepository.save(agencia);
        return AgenciaMapper.toResponse(agencia);
    }

    public void eliminar(Long id) {
        if (!agenciaRepository.existsById(id)) {
            throw new ResourceNotFoundException("No existe una agencia con id: " + id);
        }
        agenciaRepository.deleteById(id);
    }

    private String generarSlugUnico(String nombre) {
        String base = normalizarSlug(nombre);
        String slug = base;
        int contador = 1;
        while (agenciaRepository.existsBySlug(slug)) {
            slug = base + "-" + contador;
            contador++;
        }
        return slug;
    }

    private String normalizarSlug(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return sinAcentos.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}
