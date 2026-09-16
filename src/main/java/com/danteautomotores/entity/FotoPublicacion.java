package com.danteautomotores.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fotos_publicacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FotoPublicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(nullable = false)
    private String url;

    private Integer orden;
}
