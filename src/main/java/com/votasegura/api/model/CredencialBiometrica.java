package com.votasegura.api.model;

import jakarta.persistence.*;
import com.votasegura.api.security.Aes256Converter;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "credenciales_biometricas")
public class CredencialBiometrica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "credential_id", nullable = false, unique = true, length = 500)
    private String credentialId;

    @Convert(converter = Aes256Converter.class)
    @Column(name = "clave_publica", nullable = false, columnDefinition = "TEXT")
    private String clavePublica;

    @Column(name = "tipo_autenticador", nullable = false, length = 30)
    private String tipoAutenticador;

    @Column(nullable = false)
    private long contador = 0;

    @Column(length = 500)
    private String dispositivo;

    @CreationTimestamp
    @Column(name = "registrado_en", updatable = false)
    private LocalDateTime registradoEn;
}

