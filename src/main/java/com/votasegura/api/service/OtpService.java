package com.votasegura.api.service;

import com.votasegura.api.dto.request.OtpSolicitarRequest;
import com.votasegura.api.dto.request.OtpVerificarRequest;
import com.votasegura.api.dto.response.OtpResponse;
import com.votasegura.api.dto.response.OtpVerificadoResponse;
import com.votasegura.api.exception.ApiException;
import com.votasegura.api.model.OtpCode;
import com.votasegura.api.model.PasswordResetToken;
import com.votasegura.api.repository.OtpCodeRepository;
import com.votasegura.api.repository.PasswordResetTokenRepository;
import com.votasegura.api.repository.UsuarioRepository;
import com.votasegura.api.util.OtpGenerador;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpGenerador otpGenerador;
    private final JavaMailSender mailSender;
    private final EmailValidationService emailValidationService;

    @Value("${otp.expiration-seconds}")
    private int expirationSeconds;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public OtpResponse solicitar(OtpSolicitarRequest req) {
        String destinatario = "correo".equals(req.getCanal()) ? req.getCorreo() : req.getTelefono();

        // Validar correo real mediante la API REST externa usando RestTemplate
        if ("correo".equals(req.getCanal())) {
            emailValidationService.validarEmailConApiExterna(req.getCorreo());
        }

        // Validar que el usuario existe
        if ("correo".equals(req.getCanal())) {
            usuarioRepository.findByCorreo(req.getCorreo())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "No existe una cuenta asociada al correo proporcionado.", "USER_NOT_FOUND"));
        }

        // Rate limiting: no mas de 1 solicitud por destinatario/proposito cada 3 minutos
        long tresMinutosAtras = Instant.now().getEpochSecond() - 180;
        boolean yaEnviado = otpCodeRepository.existsByDestinatarioAndPropositoAndExpiraEpochGreaterThan(
                destinatario, req.getProposito(), tresMinutosAtras + expirationSeconds - 300);
        if (yaEnviado) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Ya se envio un codigo recientemente. Intentalo de nuevo en 180 segundos.",
                    "OTP_REQUEST_RATE_LIMIT_EXCEEDED");
        }

        // Generar OTP y guardar usando epoch para evitar problemas de timezone
        String codigo = otpGenerador.generar();
        long ahora = Instant.now().getEpochSecond();
        long expiraEpoch = ahora + expirationSeconds;

        OtpCode otp = new OtpCode();
        otp.setCodigo(codigo);
        otp.setProposito(req.getProposito());
        otp.setDestinatario(destinatario);
        otp.setCanal(req.getCanal());
        otp.setExpiraEpoch(expiraEpoch);
        otp.setExpiraEn(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expirationSeconds));

        OtpCode saved = otpCodeRepository.save(otp);
        log.info("OTP creado - id: {}, expiraEpoch: {}, ahoraEpoch: {}", saved.getId(), expiraEpoch, ahora);

        // Enviar correo
        if ("correo".equals(req.getCanal())) {
            enviarCorreo(req.getCorreo(), codigo, req.getProposito());
            emailValidationService.notificarViaRestApi(req.getCorreo(), "Tu codigo OTP", "Codigo: " + codigo);
        }

        return new OtpResponse("Codigo de verificacion enviado correctamente.",
                saved.getId(), req.getCanal(), expirationSeconds);
    }

    @Transactional
    public OtpVerificadoResponse verificar(OtpVerificarRequest req) {
        // Buscar SOLO por ID - todas las validaciones se hacen en Java
        OtpCode otp = otpCodeRepository.findById(req.getOtpId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "El codigo de verificacion no existe.", "OTP_NOT_FOUND"));

        log.info("OTP encontrado - id: {}, usado: {}, invalidado: {}, expiraEpoch: {}, ahoraEpoch: {}",
                otp.getId(), otp.isUsado(), otp.isInvalidado(),
                otp.getExpiraEpoch(), Instant.now().getEpochSecond());

        // Verificar si ya fue usado
        if (otp.isUsado()) {
            throw new ApiException(HttpStatus.GONE,
                    "Este codigo ya fue utilizado. Solicita uno nuevo.", "OTP_ALREADY_USED");
        }

        // Verificar si fue invalidado
        if (otp.isInvalidado()) {
            throw new ApiException(HttpStatus.GONE,
                    "Este codigo fue invalidado. Solicita uno nuevo.", "OTP_INVALIDATED");
        }

        // Verificar expiracion con epoch seconds (sin dependencia de zona horaria)
        long ahoraEpoch = Instant.now().getEpochSecond();
        if (otp.getExpiraEpoch() == null || ahoraEpoch > otp.getExpiraEpoch()) {
            throw new ApiException(HttpStatus.GONE,
                    "El codigo de verificacion ha expirado. Solicita uno nuevo.", "OTP_EXPIRED");
        }

        // Verificar intentos fallidos
        if (otp.getIntentosFallidos() >= maxAttempts) {
            otp.setInvalidado(true);
            otpCodeRepository.save(otp);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Se supero el numero de intentos permitidos. Solicita uno nuevo.",
                    "OTP_TOO_MANY_ATTEMPTS");
        }

        // Verificar el codigo
        if (!otp.getCodigo().equals(req.getCodigo())) {
            otp.setIntentosFallidos(otp.getIntentosFallidos() + 1);
            otpCodeRepository.save(otp);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "El codigo ingresado no es correcto.", "INVALID_OTP_CODE");
        }

        // Marcar como usado
        otp.setUsado(true);
        otpCodeRepository.save(otp);

        // Si es verificacion de cuenta, activar al usuario
        if ("verificacion_cuenta".equals(otp.getProposito()) && "correo".equals(otp.getCanal())) {
            usuarioRepository.findByCorreo(otp.getDestinatario()).ifPresent(usuario -> {
                usuario.setCorreoVerificado(true);
                usuario.setEstado("activo");
                usuarioRepository.save(usuario);
            });
        }

        String verificationToken = "vt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // Si es recuperacion de contrasena, guardar el token para que AuthService.restablecerPassword lo valide
        if ("recuperacion_password".equals(otp.getProposito()) && "correo".equals(otp.getCanal())) {
            usuarioRepository.findByCorreo(otp.getDestinatario()).ifPresent(usuario -> {
                PasswordResetToken prt = new PasswordResetToken();
                prt.setUsuario(usuario);
                prt.setToken(verificationToken);
                prt.setExpiraEn(LocalDateTime.now().plusMinutes(15));
                prt.setUsado(false);
                passwordResetTokenRepository.save(prt);
                log.info("PasswordResetToken generado exitosamente para {}: {}", usuario.getCorreo(), verificationToken);
            });
        }

        return new OtpVerificadoResponse(true, verificationToken, 600);
    }

    private void enviarCorreo(String correo, String codigo, String proposito) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "VotaSegura"));
            helper.setTo(correo);

            boolean esRecuperacion = "recuperacion_password".equals(proposito);
            String asunto = esRecuperacion
                    ? "VotaSegura - Restablecimiento de contraseña"
                    : "VotaSegura - Tu código de verificación";
            helper.setSubject(asunto);

            String titulo = esRecuperacion
                    ? "Recuperación de Contraseña"
                    : "Código de Verificación";

            String subtitulo = esRecuperacion
                    ? "Hemos recibido una solicitud para restablecer tu contraseña. Usa el siguiente código para continuar:"
                    : "Ingresa el siguiente código de 6 dígitos en la aplicación para verificar tu identidad:";

            // Version Texto Plano (vital para que Gmail/Outlook no lo marquen como spam)
            String plainText = "VotaSegura\n\n"
                    + titulo + "\n\n"
                    + subtitulo + "\n\n"
                    + "Código: " + codigo + "\n\n"
                    + "Este código es válido durante 5 minutos.\n"
                    + "Por tu seguridad, nunca compartas este código con nadie.\n\n"
                    + "Equipo VotaSegura";

            // Version HTML elegante con tarjeta, tipografia y diseno
            String htmlText = construirHtml(titulo, subtitulo, codigo);

            // Con setText(plainText, htmlText) se envia como multipart/alternative (maxima reputacion de entrega)
            helper.setText(plainText, htmlText);

            mailSender.send(mimeMessage);
            log.info("Correo elegante enviado exitosamente a: {}", correo);
        } catch (Exception e) {
            log.error("Error al enviar correo elegante a {}: {}", correo, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al enviar el correo. Intentalo de nuevo.", "EMAIL_SEND_ERROR");
        }
    }

    private String construirHtml(String titulo, String subtitulo, String codigo) {
        return "<!DOCTYPE html>"
                + "<html lang=\"es\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
                + "<body style=\"margin:0;padding:0;background-color:#F8F9FD;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;\">"
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#F8F9FD;padding:40px 15px;\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:500px;background-color:#FFFFFF;border-radius:20px;box-shadow:0 10px 25px rgba(108,92,231,0.08);border:1px solid #ECECF4;overflow:hidden;\">"
                + "<tr><td style=\"background:linear-gradient(135deg, #6C5CE7 0%, #8E78FF 100%);padding:32px 25px;text-align:center;\">"
                + "<h1 style=\"color:#FFFFFF;margin:0;font-size:24px;font-weight:700;letter-spacing:-0.5px;\">VotaSegura</h1>"
                + "<p style=\"color:rgba(255,255,255,0.9);margin:4px 0 0;font-size:12px;letter-spacing:0.3px;\">Plataforma Criptográfica de Votación</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:35px 30px 25px 30px;text-align:center;\">"
                + "<h2 style=\"color:#1E1B2E;margin:0 0 10px;font-size:19px;font-weight:700;\">" + titulo + "</h2>"
                + "<p style=\"color:#64748B;font-size:14px;line-height:1.5;margin:0 0 25px;\">" + subtitulo + "</p>"
                + "<div style=\"background-color:#F4F0FF;border:2px dashed #6C5CE7;border-radius:14px;padding:18px 24px;margin:0 auto 20px auto;display:inline-block;\">"
                + "<span style=\"font-family:'Courier New',Courier,monospace;font-size:32px;font-weight:800;color:#6C5CE7;letter-spacing:8px;margin-left:8px;\">" + codigo + "</span>"
                + "</div>"
                + "<p style=\"color:#64748B;font-size:12px;margin:0 0 25px;\">Código de un solo uso válido por <strong>5 minutos</strong>.</p>"
                + "<div style=\"background-color:#FEF3C7;border-left:4px solid #F59E0B;padding:12px 14px;border-radius:8px;text-align:left;margin-bottom:25px;\">"
                + "<p style=\"color:#92400E;font-size:11px;line-height:1.5;margin:0;\">"
                + "<strong>Aviso de Seguridad:</strong> No compartas este código con nadie. El equipo de VotaSegura nunca te solicitará tu código."
                + "</p></div>"
                + "<p style=\"color:#94A3B8;font-size:11px;margin:0;line-height:1.4;\">Si no solicitaste este código, puedes ignorar este mensaje. Tu cuenta permanece protegida.</p>"
                + "</td></tr>"
                + "<tr><td style=\"background-color:#F8F9FD;padding:18px 25px;text-align:center;border-top:1px solid #ECECF4;\">"
                + "<p style=\"color:#94A3B8;font-size:11px;margin:0;\">© 2026 VotaSegura • Seguridad Informática</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }
}
