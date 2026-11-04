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
@Table(name = "registro_votos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eleccion_id", "usuario_id"})
})
public class RegistroVoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleccion_id", nullable = false)
    private Eleccion eleccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo_recibo", nullable = false, length = 30)
    private String codigoRecibo;

    @CreationTimestamp
    @Column(name = "emitido_en", updatable = false)
    private LocalDateTime emitidoEn;
}