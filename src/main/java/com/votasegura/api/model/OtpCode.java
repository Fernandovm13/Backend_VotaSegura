package com.votasegura.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "otp_codes")
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(nullable = false, length = 50)
    private String proposito;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Column(nullable = false, length = 10)
    private String canal;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    // Tiempo de expiracion en epoch seconds (Unix timestamp)
    // Independiente de zona horaria - soluciona problemas de timezone con MySQL
    @Column(name = "expira_epoch")
    private Long expiraEpoch;

    @Column(nullable = false)
    private int intentosFallidos = 0;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(nullable = false)
    private boolean invalidado = false;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;
}