package com.danteautomotores.entity;

import com.danteautomotores.enums.Combustible;
import com.danteautomotores.enums.Condicion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.Transmision;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publicaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agencia_id", nullable = false)
    private Agencia agencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Usuario admin;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Builder.Default
    private String moneda = "ARS";

    private Integer kilometraje;

    @Enumerated(EnumType.STRING)
    private Transmision transmision;

    @Enumerated(EnumType.STRING)
    private Combustible combustible;

    private String color;

    @Enumerated(EnumType.STRING)
    private Condicion condicion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoPublicacion estado = EstadoPublicacion.DISPONIBLE;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FotoPublicacion> fotos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaPublicacion = LocalDateTime.now();
    }
}
