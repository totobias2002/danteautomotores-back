package com.danteautomotores.entity;

import com.danteautomotores.enums.EstadoSolicitudVenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Solicitud de un usuario para vender su auto a través del flujo "Vender tu
// auto" (cotizador). A diferencia de Consulta, acá no hay una Publicacion
// existente para relacionar: el auto todavía no es del stock de la agencia,
// así que sus datos se guardan directamente en la solicitud.
@Entity
@Table(name = "solicitudes_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    private Integer kilometraje;

    @Column(name = "cotizacion_min")
    private BigDecimal cotizacionMin;

    @Column(name = "cotizacion_max")
    private BigDecimal cotizacionMax;

    @Column(name = "nombre_vendedor", nullable = false)
    private String nombreVendedor;

    @Column(name = "telefono_vendedor", nullable = false)
    private String telefonoVendedor;

    private String ciudad;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudVenta estado;

    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoSolicitudVenta.PENDIENTE;
        }
    }
}
