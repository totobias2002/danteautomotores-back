package com.danteautomotores.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String slug;

    private String logo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String direccion;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    @Column(name = "email_contacto")
    private String emailContacto;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @PrePersist
    protected void onCreate() {
        this.fechaAlta = LocalDateTime.now();
    }
}
