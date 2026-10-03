package com.danteautomotores.entity;

import com.danteautomotores.enums.Combustible;
import com.danteautomotores.enums.Condicion;
import com.danteautomotores.enums.EstadoPublicacion;
import com.danteautomotores.enums.TipoCarroceria;
import com.danteautomotores.enums.Transmision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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
    @Column(name = "tipo_carroceria")
    private TipoCarroceria tipoCarroceria;

    // Precio previo para la regla de oferta (D-03): es oferta si es mayor que el precio actual.
    @Column(name = "precio_anterior", precision = 12, scale = 2)
    private BigDecimal precioAnterior;

    @Enumerated(EnumType.STRING)
    private Condicion condicion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoPublicacion estado = EstadoPublicacion.DISPONIBLE;

    // @ColumnDefault: sin default, ddl-auto: update genera "boolean not null" y PostgreSQL no puede
    // agregar la columna a una tabla que ya tiene filas. @Builder.Default: sin él el builder ignora el "= false".
    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean destacado = false;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    // La fija solo el servidor al pasar a VENDIDO (D-04); no viaja en ningun request.
    @Column(name = "fecha_vendido")
    private LocalDateTime fechaVendido;

    // Hace determinista el orden de carga para cualquier código que recorra la colección sin ordenar. PostgreSQL pone
    // los orden null al final; la regla que decide la portada es PublicacionMapper.ORDEN_DE_FOTOS.
    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    @Builder.Default
    private List<FotoPublicacion> fotos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaPublicacion = LocalDateTime.now();
    }
}
