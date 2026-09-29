package com.danteautomotores.repository;

import com.danteautomotores.entity.SolicitudVenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudVentaRepository extends JpaRepository<SolicitudVenta, Long> {
    List<SolicitudVenta> findAllByOrderByFechaDesc();
}
