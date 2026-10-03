package com.danteautomotores.service;

import com.danteautomotores.dto.publicacion.CambiarDestacadoRequest;
import com.danteautomotores.dto.publicacion.CambiarEstadoRequest;
import com.danteautomotores.dto.publicacion.ImpactoEliminacionResponse;
import com.danteautomotores.dto.publicacion.PublicacionRequest;
import com.danteautomotores.dto.publicacion.PublicacionResponse;
import com.danteautomotores.dto.publicacion.ReordenarFotosRequest;
import com.danteautomotores.entity.Agencia;
import com.danteautomotores.entity.FotoPublicacion;
import com.danteautomotores.entity.Publicacion;
import com.danteautomotores.entity.Usuario;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.exception.ReglaDeNegocioException;
import com.danteautomotores.exception.ResourceNotFoundException;
import com.danteautomotores.mapper.PublicacionMapper;
import com.danteautomotores.repository.AgenciaRepository;
import com.danteautomotores.repository.ConsultaRepository;
import com.danteautomotores.repository.FavoritoRepository;
import com.danteautomotores.repository.FotoPublicacionRepository;
import com.danteautomotores.repository.PublicacionRepository;
import com.danteautomotores.repository.UsuarioRepository;
import com.danteautomotores.repository.spec.PublicacionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final AgenciaRepository agenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FotoPublicacionRepository fotoPublicacionRepository;
    private final ConsultaRepository consultaRepository;
    private final FavoritoRepository favoritoRepository;
    private final CloudinaryService cloudinaryService;
    private final ImagenValidator imagenValidator;

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

    // Listado del panel admin: todos los estados (el catálogo público sigue filtrando DISPONIBLE en buscar()),
    // los más recientes primero.
    @Transactional(readOnly = true)
    public List<PublicacionResponse> listarParaAdmin() {
        return publicacionRepository
                .findAll(Sort.by(Sort.Direction.DESC, "fechaPublicacion"))
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

    // destacado es independiente del estado: solo este método lo modifica (ni crear, ni actualizar,
    // ni cambiarEstado lo tocan).
    @Transactional
    public PublicacionResponse cambiarDestacado(Long id, CambiarDestacadoRequest request) {
        Publicacion publicacion = buscarEntidad(id);
        publicacion.setDestacado(request.getDestacado());
        publicacionRepository.save(publicacion);
        return PublicacionMapper.toResponse(publicacion);
    }

    // Decisión del usuario (cascada + aviso): los favoritos y las consultas de la publicación se borran en la
    // misma transacción, y el panel avisa antes cuántos son (obtenerImpactoEliminacion). Las fotos caen por
    // cascade/orphanRemoval. Si algo falla no queda nada borrado a medias.
    @Transactional
    public void eliminar(Long id) {
        Publicacion publicacion = buscarEntidad(id);

        List<String> publicIds = publicacion.getFotos().stream()
                .map(FotoPublicacion::getPublicId)
                .toList();

        favoritoRepository.deleteByPublicacionId(id);
        consultaRepository.deleteByPublicacionId(id);
        publicacionRepository.delete(publicacion);

        eliminarImagenesDespuesDelCommit(publicIds);
    }

    @Transactional(readOnly = true)
    public ImpactoEliminacionResponse obtenerImpactoEliminacion(Long id) {
        if (!publicacionRepository.existsById(id)) {
            throw new ResourceNotFoundException("No existe una publicación con id: " + id);
        }
        return ImpactoEliminacionResponse.builder()
                .cantidadConsultas(consultaRepository.countByPublicacionId(id))
                .cantidadFavoritos(favoritoRepository.countByPublicacionId(id))
                .build();
    }

    @Transactional
    public PublicacionResponse agregarFoto(Long id, MultipartFile archivo) {
        Publicacion publicacion = buscarEntidadParaEscritura(id);

        // Las dos validaciones van antes de subir: una foto rechazada nunca llega a Cloudinary.
        imagenValidator.validar(archivo);
        if (publicacion.getFotos().size() >= ImagenValidator.MAX_FOTOS) {
            throw new ReglaDeNegocioException("Cada auto puede tener hasta 10 fotos");
        }

        CloudinaryService.ImagenSubida subida = cloudinaryService.subir(archivo);
        // Desde acá la imagen ya está en Cloudinary: si la transacción no termina en commit, se borra.
        eliminarImagenSiNoHayCommit(subida.publicId());

        // Máximo orden actual + 1 (y no size()): si se borró una foto del medio, size() repetiría un orden.
        int siguienteOrden = publicacion.getFotos().stream()
                .mapToInt(f -> f.getOrden() == null ? -1 : f.getOrden())
                .max()
                .orElse(-1) + 1;

        FotoPublicacion foto = FotoPublicacion.builder()
                .publicacion(publicacion)
                .url(subida.url())
                .publicId(subida.publicId())
                .orden(siguienteOrden)
                .build();

        fotoPublicacionRepository.save(foto);
        publicacion.getFotos().add(foto);

        return PublicacionMapper.toResponse(publicacion);
    }

    // La lista debe ser exactamente el conjunto de fotos de la publicación (sin faltantes, repetidas ni ajenas),
    // así un reorden que se cruza con otra subida o borrado falla en vez de dejar un orden corrupto.
    @Transactional
    public PublicacionResponse reordenarFotos(Long id, ReordenarFotosRequest request) {
        Publicacion publicacion = buscarEntidadParaEscritura(id);

        Map<Long, FotoPublicacion> fotosPorId = publicacion.getFotos().stream()
                .collect(Collectors.toMap(FotoPublicacion::getId, Function.identity()));
        List<Long> fotoIds = request.getFotoIds();

        if (fotoIds.size() != fotosPorId.size()
                || new HashSet<>(fotoIds).size() != fotoIds.size()
                || !fotosPorId.keySet().containsAll(fotoIds)) {
            throw new ReglaDeNegocioException("El orden debe incluir todas las fotos del auto, una sola vez cada una");
        }

        // orden 0 = portada
        for (int i = 0; i < fotoIds.size(); i++) {
            fotosPorId.get(fotoIds.get(i)).setOrden(i);
        }

        return PublicacionMapper.toResponse(publicacion);
    }

    @Transactional
    public void eliminarFoto(Long publicacionId, Long fotoId) {
        // El lock va primero: las fotos se leen después de tomarlo, así el resecuenciado parte del estado vigente.
        Publicacion publicacion = buscarEntidadParaEscritura(publicacionId);

        FotoPublicacion foto = fotoPublicacionRepository.findById(fotoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la foto con id: " + fotoId));

        if (!foto.getPublicacion().getId().equals(publicacionId)) {
            throw new ReglaDeNegocioException("La foto no pertenece a esta publicación");
        }

        publicacion.getFotos().remove(foto);
        fotoPublicacionRepository.delete(foto);
        resecuenciarFotos(publicacion);

        // singletonList y no List.of: el public_id es null en las fotos viejas.
        eliminarImagenesDespuesDelCommit(Collections.singletonList(foto.getPublicId()));
    }

    // Deja las fotos que quedan en 0..n-1 sin huecos ni repetidos, con el mismo criterio de orden que
    // PublicacionMapper (null cuenta como 0, desempate por id): evita órdenes duplicados y una portada
    // indefinida después de borrar (RESEARCH Pitfall 3).
    private void resecuenciarFotos(Publicacion publicacion) {
        List<FotoPublicacion> ordenadas = publicacion.getFotos().stream()
                .sorted(Comparator
                        .comparing((FotoPublicacion f) -> f.getOrden() == null ? 0 : f.getOrden())
                        .thenComparing(FotoPublicacion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (int i = 0; i < ordenadas.size(); i++) {
            ordenadas.get(i).setOrden(i);
        }
    }

    // El borrado en Cloudinary va después del commit: si la transacción hace rollback, las filas siguen
    // apuntando a imágenes que existen (RESEARCH Pitfall 8). CloudinaryService.eliminar nunca lanza, así que un
    // fallo ahí no deshace un borrado ya confirmado. Las fotos viejas sin public_id se omiten.
    private void eliminarImagenesDespuesDelCommit(List<String> publicIds) {
        List<String> aBorrar = publicIds.stream()
                .filter(publicId -> publicId != null && !publicId.isBlank())
                .toList();
        if (aBorrar.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aBorrar.forEach(cloudinaryService::eliminar);
                }
            });
        } else {
            aBorrar.forEach(cloudinaryService::eliminar);
        }
    }

    // Compensación de la subida: si el guardado o el commit fallan, ninguna fila apunta a la imagen recién subida y
    // quedaría huérfana en Cloudinary. CloudinaryService.eliminar nunca lanza.
    private void eliminarImagenSiNoHayCommit(String publicId) {
        if (publicId == null || publicId.isBlank() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    cloudinaryService.eliminar(publicId);
                }
            }
        });
    }

    private Publicacion buscarEntidad(Long id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + id));
    }

    // Carga la publicación con lock pesimista (ver PublicacionRepository.findByIdForUpdate); solo dentro de @Transactional.
    private Publicacion buscarEntidadParaEscritura(Long id) {
        return publicacionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una publicación con id: " + id));
    }

    private Usuario obtenerUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }
}
