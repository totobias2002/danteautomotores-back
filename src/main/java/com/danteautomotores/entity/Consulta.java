package com.danteautomotores.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "nombre_comprador", nullable = false)
    private String nombreComprador;

    @Column(name = "email_comprador", nullable = false)
    private String emailComprador;

    @Column(name = "telefono_comprador")
    private String telefonoComprador;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
    }
}
