package com.danteautomotores.entity;

import com.danteautomotores.enums.EstadoConversacion;
import com.danteautomotores.enums.TipoConversacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Conversación entre un usuario y la agencia (D-01). Las fechas están en UTC (D-13) y las fija el servicio con el
 * reloj inyectado: por eso no hay @PrePersist.
 */
@Entity
@Table(name = "conversaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConversacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConversacion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Nulo en una conversación de cotización (D-02).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id")
    private Publicacion publicacion;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "ultimo_mensaje_en", nullable = false)
    private LocalDateTime ultimoMensajeEn;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    // El comprador la borró de su lista. Solo oculta: la agencia sigue viendo la conversación completa.
    @Column(name = "oculta_para_usuario", nullable = false)
    @Builder.Default
    private boolean ocultaParaUsuario = false;
}
