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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final EmailValidationService emailValidationService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ===== 3.1 Registro =====
    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        // Validacion con API externa: descartar correos temporales / desechables
        emailValidationService.validarEmailConApiExterna(req.getCorreo());
        if (usuarioRepository.existsByCorreo(req.getCorreo())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El correo electronico '" + req.getCorreo() + "' ya esta registrado.",
                    "USER_ALREADY_EXISTS");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(req.getNombre());
        usuario.setCorreo(req.getCorreo());
        usuario.setPassword(passwordEncoder.encode(req.getPassword()));
        usuario.setRol(req.getRol() != null ? req.getRol() : Rol.VOTANTE);
        usuario.setEstado("pendiente_verificacion");

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    // ===== 3.2 Login =====
    @Transactional
    public AuthResponse login(LoginRequest req) {
        // Verificar que la cuenta existe y esta verificada
        Usuario usuario = usuarioRepository.findByCorreo(req.getCorreo())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "El correo o la contrasena son incorrectos.", "INVALID_CREDENTIALS"));

                if ("inactivo".equalsIgnoreCase(usuario.getEstado()) || "desactivado".equalsIgnoreCase(usuario.getEstado())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta ha sido desactivada.", "CUENTA_DESACTIVADA");
        }

        if ("bloqueado".equals(usuario.getEstado())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tu cuenta esta bloqueada. Contacta al administrador.", "ACCOUNT_BLOCKED");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getCorreo(), req.getPassword()));
                } catch (org.springframework.security.authentication.DisabledException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu cuenta ha sido desactivada.", "CUENTA_DESACTIVADA");
        } catch (BadCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "El correo o la contrasena son incorrectos.", "INVALID_CREDENTIALS");
        }

        // Actualizar estado a activo si estaba pendiente (login implica verificacion basica)
        if ("pendiente_verificacion".equals(usuario.getEstado())) {
            usuario.setEstado("activo");
            usuarioRepository.save(usuario);
        }

        return generarAuthResponse(usuario);
    }

    // ===== 3.3 Logout =====
    @Transactional
    public MensajeResponse logout(String accessToken, LogoutRequest req) {
        // Agregar accessToken a la blacklist
        TokenBlacklist blacklistedToken = new TokenBlacklist();
        blacklistedToken.setToken(accessToken);
        blacklistedToken.setInvalidadoEn(LocalDateTime.now());
        blacklistedToken.setExpiraEn(LocalDateTime.now().plusSeconds(jwtExpirationMs / 1000));
        tokenBlacklistRepository.save(blacklistedToken);

        // Revocar el refreshToken
        refreshTokenRepository.findByToken(req.getRefreshToken())
                .ifPresent(rt -> {
                    rt.setRevocado(true);
                    refreshTokenRepository.save(rt);
                });

        return new MensajeResponse("Sesion cerrada correctamente.", LocalDateTime.now());
    }

    // ===== 3.6 Renovar Token =====
    @Transactional
    public AuthResponse renovarToken(RefreshTokenRequest req) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "El token de refresco proporcionado no es valido.", "REFRESH_TOKEN_INVALID"));

        if (refreshToken.isRevocado()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "El token de refresco fue revocado. Inicia sesion nuevamente.", "REFRESH_TOKEN_REVOKED");
        }

        if (refreshToken.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "El token de refresco ha expirado. Inicia sesion nuevamente.", "REFRESH_TOKEN_EXPIRED");
        }

        // Revocar el token actual (rotacion)
        refreshToken.setRevocado(true);
        refreshTokenRepository.save(refreshToken);

        return generarAuthResponse(refreshToken.getUsuario());
    }

    // ===== 3.8 Recuperar Password =====
    public MensajeResponse recuperarPassword(RecuperarPasswordRequest req) {
        // Siempre retornamos el mismo mensaje por seguridad (no revelar si el correo existe)
        usuarioRepository.findByCorreo(req.getCorreo()).ifPresent(usuario -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUsuario(usuario);
            resetToken.setToken(token);
            resetToken.setExpiraEn(LocalDateTime.now().plusMinutes(15));
            passwordResetTokenRepository.save(resetToken);

            enviarCorreoRecuperacion(req.getCorreo(), token);
        });

        return new MensajeResponse(
                "Si el correo se encuentra registrado, recibiras instrucciones para restablecer tu contrasena.");
    }

    // ===== 3.9 Restablecer Password =====
    @Transactional
    public MensajeResponse restablecerPassword(RestablecerPasswordRequest req) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsadoFalse(req.getResetToken())
                .orElseThrow(() -> new ApiException(HttpStatus.GONE,
                        "El enlace de restablecimiento ha expirado o ya fue utilizado. Solicita uno nuevo.",
                        "RESET_TOKEN_EXPIRED"));

        if (resetToken.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.GONE,
                    "El enlace de restablecimiento ha expirado. Solicita uno nuevo.", "RESET_TOKEN_EXPIRED");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(req.getNuevaPassword()));
        usuarioRepository.save(usuario);

        // Revocar todos los refresh tokens activos
        refreshTokenRepository.revocarTodosPorUsuario(usuario);

        // Marcar el reset token como usado
        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);

        return new MensajeResponse("Contrasena actualizada correctamente. Se cerraron todas las sesiones activas.");
    }

    // ===== Metodo auxiliar: generar tokens =====
    private AuthResponse generarAuthResponse(Usuario usuario) {
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

            private void enviarCorreoRecuperacion(String correo, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(correo);
            message.setSubject("VotaSegura - Recuperacion de contrasena");
            message.setText(
                "Hola,\n\n" +
                "Recibimos una solicitud para restablecer tu contrasena.\n\n" +
                "Usa el siguiente token para restablecerla:\n" +
                token + "\n\n" +
                "Este enlace es valido por 15 minutos.\n\n" +
                "Si no solicitaste este cambio, ignora este mensaje.\n\n" +
                "Equipo VotaSegura"
            );
            mailSender.send(message);
        } catch (Exception e) {
            // No lanzar excepcion para no revelar si el correo existe
        }
    }
}
