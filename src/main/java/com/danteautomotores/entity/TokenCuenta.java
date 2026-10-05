package com.danteautomotores.entity;

import com.danteautomotores.enums.TipoTokenCuenta;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Token de un solo uso para confirmar el mail o restablecer la contraseña. En la base vive solo el SHA-256 del token
 * (el valor que viaja en el link nunca se guarda). Sin {@code @Data} ni {@code @ToString}: no imprime el hash.
 * {@code usuarioId} es una columna simple, sin relación JPA, para no cargar la cuenta al consumir.
 */
@Entity
@Table(name = "tokens_cuenta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoTokenCuenta tipo;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado_en")
    private LocalDateTime usadoEn;
}
