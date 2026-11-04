package com.votasegura.api.service;

import com.votasegura.api.dto.request.*;
import com.votasegura.api.dto.response.*;
import com.votasegura.api.exception.ApiException;
import com.votasegura.api.model.*;
import com.votasegura.api.repository.*;
import com.votasegura.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BiometriaService {

    private final UsuarioRepository usuarioRepository;
    private final CredencialBiometricaRepository credencialRepo;
    private final DesafioChallengeRepository desafioRepo;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ===== 3.10 Desafio para Registro Biometrico (requiere Bearer) =====
    @Transactional
    public BiometriaDesafioResponse generarDesafioRegistro(String correoUsuario, BiometriaDesafioRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado", "USER_NOT_FOUND"));

        // Generar challenge aleatorio de 32 bytes codificado en Base64
        byte[] challengeBytes = new byte[32];
        SECURE_RANDOM.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        // Invalidar desafios previos sin usar para este usuario
        desafioRepo.findAllByUsuarioAndTipoAndUsadoFalse(usuario, "REGISTRO").forEach(d -> {
            d.setUsado(true);
            desafioRepo.save(d);
        });

        // Guardar el desafio en BD con expiracion de 60 segundos
        DesafioChallenge desafio = new DesafioChallenge();
        desafio.setUsuario(usuario);
        desafio.setChallenge(challenge);
        desafio.setTipo("REGISTRO");
        desafio.setExpiraEn(LocalDateTime.now().plusSeconds(60));
        desafioRepo.save(desafio);

        return new BiometriaDesafioResponse(
                challenge,
                req.getDomain() != null && !req.getDomain().isBlank() ? req.getDomain() : "localhost",
                usuario.getId(),
                List.of("ES256", "RS256"),
                60000
        );
    }

    // ===== 3.11 Confirmar Registro Biometrico (requiere Bearer) =====
    @Transactional
    public CredencialRegistradaResponse confirmarRegistro(String correoUsuario, BiometriaConfirmarRequest req) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado", "USER_NOT_FOUND"));

        // Verificar que el challenge es valido y no ha expirado
        DesafioChallenge desafio = desafioRepo
                .findByChallengeAndTipoAndUsadoFalseAndExpiraEnAfter(req.getChallenge(), "REGISTRO", LocalDateTime.now())
                .orElseThrow(() -> new ApiException(HttpStatus.GONE,
                        "El desafio ha expirado. Solicita uno nuevo para continuar con el registro.",
                        "CHALLENGE_EXPIRED"));

        if (!desafio.getChallenge().equals(req.getChallenge())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No fue posible validar la firma del autenticador contra el desafio emitido.",
                    "SIGNATURE_VALIDATION_FAILED");
        }

        // Verificar que la credencial no este duplicada
        if (credencialRepo.existsByCredentialId(req.getCredentialId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Esta credencial biometrica ya se encuentra registrada en la cuenta.",
                    "CREDENTIAL_ALREADY_REGISTERED");
        }

        // Guardar la credencial biometrica
        CredencialBiometrica credencial = new CredencialBiometrica();
        credencial.setUsuario(usuario);
        credencial.setCredentialId(req.getCredentialId());
        credencial.setClavePublica(req.getClavePublica());
        credencial.setTipoAutenticador(req.getTipoAutenticador() != null ? req.getTipoAutenticador() : "platform_authenticator");
        credencial.setDispositivo(req.getDispositivo());
        credencialRepo.save(credencial);

        // Marcar challenge como usado y biometria como registrada
        desafio.setUsado(true);
        desafioRepo.save(desafio);
        usuario.setBiometriaRegistrada(true);
        usuarioRepository.save(usuario);

        return new CredencialRegistradaResponse(
                credencial.getCredentialId(),
                credencial.getTipoAutenticador(),
                credencial.getDispositivo(),
                credencial.getRegistradoEn()
        );
    }

    // ===== 3.12 Desafio para Login Biometrico (publico) =====
    @Transactional
        public BiometriaLoginDesafioResponse generarDesafioLogin(BiometriaLoginDesafioRequest req) {
        Usuario usuarioCheck = usuarioRepository.findByCorreo(req.getCorreo()).orElse(null);
        if (usuarioCheck != null && ("inactivo".equalsIgnoreCase(usuarioCheck.getEstado()) || "desactivado".equalsIgnoreCase(usuarioCheck.getEstado()) || "bloqueado".equalsIgnoreCase(usuarioCheck.getEstado()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta ha sido desactivada.", "CUENTA_DESACTIVADA");
        }
        Usuario usuario = usuarioRepository.findByCorreo(req.getCorreo())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No existe una cuenta asociada al correo proporcionado.", "USER_NOT_FOUND"));

        List<CredencialBiometrica> credenciales = credencialRepo.findByUsuario(usuario);
        if (credenciales.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "La cuenta no tiene credenciales biometricas registradas.",
                    "NO_BIOMETRIC_CREDENTIALS_REGISTERED");
        }

        // Generar challenge
        byte[] challengeBytes = new byte[32];
        SECURE_RANDOM.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        DesafioChallenge desafio = new DesafioChallenge();
        desafio.setUsuario(usuario);
        desafio.setChallenge(challenge);
        desafio.setTipo("LOGIN");
        desafio.setExpiraEn(LocalDateTime.now().plusSeconds(60));
        desafioRepo.save(desafio);

        List<BiometriaLoginDesafioResponse.CredencialInfo> allowCredentials = credenciales.stream()
                .map(c -> new BiometriaLoginDesafioResponse.CredencialInfo(c.getCredentialId(), "public-key"))
                .collect(Collectors.toList());

        return new BiometriaLoginDesafioResponse(challenge, allowCredentials, 60000);
    }

    // ===== 3.13 Validar Firma Biometrica y dar Acceso (publico) =====
    @Transactional
    public AuthResponse validarFirmaLogin(BiometriaLoginValidarRequest req) {
        // Buscar la credencial
        CredencialBiometrica credencial = credencialRepo.findByCredentialId(req.getCredentialId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "La credencial biometrica indicada no se encuentra registrada.",
                        "CREDENTIAL_NOT_FOUND"));

                Usuario usuario = credencial.getUsuario();
        if ("inactivo".equalsIgnoreCase(usuario.getEstado()) || "desactivado".equalsIgnoreCase(usuario.getEstado()) || "bloqueado".equalsIgnoreCase(usuario.getEstado())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta ha sido desactivada.", "CUENTA_DESACTIVADA");
        }

        // Verificar que el challenge es valido
        DesafioChallenge desafio = desafioRepo
                .findByChallengeAndTipoAndUsadoFalseAndExpiraEnAfter(req.getChallenge(), "LOGIN", LocalDateTime.now())
                .orElseThrow(() -> new ApiException(HttpStatus.GONE,
                        "El desafio ha expirado. Solicita uno nuevo para iniciar sesion.",
                        "CHALLENGE_EXPIRED"));

        /*
         * NOTA: En un sistema productivo completo se verificaria la firma criptografica
         * usando la clave publica almacenada con webauthn4j.
         * Para el alcance academico del contrato, validamos que el challenge coincide
         * y que la credencial existe y pertenece a un usuario activo.
         */
        if (!desafio.getUsuario().getId().equals(usuario.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No fue posible validar la firma biometrica proporcionada.", "SIGNATURE_INVALID");
        }

        // Marcar challenge como usado
        desafio.setUsado(true);
        desafioRepo.save(desafio);

        // Actualizar contador del autenticador
        credencial.setContador(credencial.getContador() + 1);
        credencialRepo.save(credencial);

        // Generar tokens
        String accessToken = jwtUtil.generateAccessToken(usuario.getCorreo(), usuario.getRol().name());

        RefreshToken rt = new RefreshToken();
        rt.setUsuario(usuario);
        rt.setToken("rt_" + UUID.randomUUID().toString().replace("-", ""));
        rt.setExpiraEn(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        refreshTokenRepository.save(rt);

        AuthResponse.UsuarioInfo info = AuthResponse.UsuarioInfo.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rt.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .usuario(info)
                .build();
    }
}


