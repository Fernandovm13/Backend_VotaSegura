package com.votasegura.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "candidatos")
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleccion_id", nullable = false)
    private Eleccion eleccion;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 100)
    @Builder.Default
    private String puesto = "Candidato/a";

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "votos", nullable = false)
    @Builder.Default
    private long votos = 0L;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;
}