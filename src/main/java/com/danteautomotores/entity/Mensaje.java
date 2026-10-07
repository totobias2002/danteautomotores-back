package com.danteautomotores.entity;

import com.danteautomotores.enums.AutorMensaje;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Mensaje de texto plano (D-14) de una conversación. Fechas en UTC (D-13). */
@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    // La cuenta que lo escribió. Del lado de la agencia es el admin, pero el usuario ve "Dante Automotores" (D-05).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @Column(name = "autor_tipo", nullable = false, length = 20)
    private AutorMensaje autorTipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "leido_en")
    private LocalDateTime leidoEn;
}
