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
@Table(name = "miembros_grupo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"grupo_id", "usuario_id"})
})
public class MiembroGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "rol_en_grupo", nullable = false, length = 25)
    @Builder.Default
    private String rolEnGrupo = "votante";

    @CreationTimestamp
    @Column(name = "unido_en", updatable = false)
    private LocalDateTime unidoEn;
}