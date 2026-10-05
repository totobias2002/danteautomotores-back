package com.danteautomotores.entity;

import com.danteautomotores.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    // Nulo en una cuenta que solo entra con Google.
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    private String telefono;

    // Nulo en las cuentas anteriores a la Fase 3: la obligatoriedad vive en VerificacionCuenta, no en la base.
    private String apellido;

    // Sin puntos, 7 u 8 dígitos. Único. No se imprime: la entidad no usa @Data ni @ToString.
    @Column(length = 8)
    private String dni;

    @Column(name = "email_confirmado", nullable = false)
    private boolean emailConfirmado;

    @Column(name = "google_sub")
    private String googleSub;

    // Los JWT emitidos antes de esta fecha dejan de valer (cambio o restablecimiento de contraseña).
    @Column(name = "password_cambiada_en")
    private LocalDateTime passwordCambiadaEn;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }
}
