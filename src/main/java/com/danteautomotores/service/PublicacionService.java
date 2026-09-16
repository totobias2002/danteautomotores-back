package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.CambiarEstadoRequest;
import com.danteautomotores.dto.publicacion.PublicacionRequest;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.FotoPublicacion;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.PublicacionMapper;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.FotoPublicacionRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.repository.spec.PublicacionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final AgenciaRepository agenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FotoPublicacionRepository fotoPublicacionRepository;
    private final CloudinaryService cloudinaryService;

    public List<PublicacionResponse> buscar(String marca, String modelo, Integer anioMin, Integer anioMax,
                                             BigDecimal precioMin, BigDecimal precioMax,
                                             EstadoPublicacion estado, Long agenciaId) {
        // Si no se pide un estado puntual, por defecto solo se muestran los autos disponibles
        // (para que un admin vea también los vendidos/reservados, que pase ?estado=VENDIDO explícitamente).
        EstadoPublicacion estadoFiltro = estado != null ? estado : EstadoPublicacion.DISPONIBLE;

        return publicacionRepository
                .findAll(PublicacionSpecification.conFiltros(marca, modelo, anioMin, anioMax, precioMin, precioMax, estadoFiltro, agenciaId))
                .stream()
                .map(PublicacionMapper::toResponse)
                .toList();
    }

    public PublicacionResponse obtenerPorId(Long id) {
        return PublicacionMapper.toResponse(buscarEntidad(id));
    }

    public PublicacionResponse crear(PublicacionRequest request) {
        Agencia agencia = agenciaRepository.findById(request.getAgenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe una agencia con id: " + request.getAgenciaId()));

        Publicacion publicacion = Publicacion.builder()
                .agencia(agencia)
                .admin(obtenerUsuarioAutenticado())
                .marca(request.getMarca())
                .modelo(request.getModelo())
                .anio(request.getAnio())
                .precio(request.getPrecio())
                .moneda(request.getMoneda() != null ? request.getMoneda() : "ARS")
                .kilometraje(request.getKilometraje())
                .transmision(request.getTransmision())
                .combustible(request.getCombustible())
                .color(request.getColor())
                .condicion(request.getCondicion())
                .descripcion(request.getDescripcion())
                .build();

        publicacionRepository.save(publicacion);
        return PublicacionMapper.toResponse(publicacion);
    }

    public PublicacionResponse actualizar(Long id, PublicacionRequest request) {
        Publicacion publicacion = buscarEntidad(id);

        Agencia agencia = agenciaRepository.findById(request.getAgenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe una agencia con id: " + request.getAgenciaId()));

        publicacion.setAgencia(agencia);
        publicacion.setMarca(request.getMarca());
        publicacion.setModelo(request.getModelo());
        publicacion.setAnio(request.getAnio());
        publicacion.setPrecio(request.getPrecio());
        publicacion.setMoneda(request.getMoneda() != null ? request.getMoneda() : publicacion.getMoneda());
        publicacion.setKilometraje(request.getKilometraje());
        publicacion.setTransmision(request.getTransmision());
        publicacion.setCombustible(request.getCombustible());
        publicacion.setColor(request.getColor());
        publicacion.setCondicion(request.getCondicion());
        publicacion.setDescripcion(request.getDescripcion());

        publicacionRepository.save(publicacion);
        return PublicacionMapper.toResponse(publicacion);
    }

    public PublicacionResponse cambiarEstado(Long id, CambiarEstadoRequest request) {
        Publicacion publicacion = buscarEntidad(id);
        publicacion.setEstado(request.getEstado());
        publicacionRepository.save(publicacion);
        return PublicacionMapper.toResponse(publicacion);
    }

    public void eliminar(Long id) {
        if (!publicacionRepository.existsById(id)) {
            throw new ResourceNotFoundException("No existe una publicación con id: " + id);
        }
        publicacionRepository.deleteById(id);
    }

    public PublicacionResponse agregarFoto(Long id, MultipartFile archivo) {
        Publicacion publicacion = buscarEntidad(id);

        String url = cloudinaryService.subirImagen(archivo);
        int siguienteOrden = publicacion.getFotos().size();

        FotoPublicacion foto = FotoPublicacion.builder()
                .publicacion(publicacion)
                .url(url)
                .orden(siguienteOrden)
                .build();

        fotoPublicacionRepository.save(foto);
        publicacion.getFotos().add(foto);

        return PublicacionMapper.toResponse(publicacion);
    }

    public void eliminarFoto(Long publicacionId, Long fotoId) {
        FotoPublicacion foto = fotoPublicacionRepository.findById(fotoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la foto con id: " + fotoId));

        if (!foto.getPublicacion().getId().equals(publicacionId)) {
            throw new IllegalArgumentException("La foto no pertenece a esta publicación");
        }

        fotoPublicacionRepository.delete(foto);
    }

    private Publicacion buscarEntidad(Long id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + id));
    }

    private Usuario obtenerUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }
}
