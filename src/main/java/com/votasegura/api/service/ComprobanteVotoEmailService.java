package com.votasegura.api.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteVotoEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy, HH:mm 'hrs'", new Locale("es", "MX"));

    public void enviarComprobanteVotoAsync(
            String correoDestinatario,
            String nombreVotante,
            String tituloEleccion,
            String nombreGrupo,
            String candidatoNombre,
            String candidatoCargo,
            String codigoRecibo,
            LocalDateTime fechaEmision) {

        CompletableFuture.runAsync(() -> {
            try {
                if (correoDestinatario == null || correoDestinatario.isBlank()) {
                    log.warn("No se puede enviar comprobante: correo destinatario vacio");
                    return;
                }

                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(new InternetAddress(fromEmail, "VotaSegura"));
                helper.setTo(correoDestinatario);
                helper.setSubject("Comprobante de Voto: " + tituloEleccion);

                LocalDateTime fechaFinal = fechaEmision != null ? fechaEmision : LocalDateTime.now();
                String fechaFormateada = fechaFinal.format(FORMATO_FECHA_HORA);
                String nombreMostrar = (nombreVotante != null && !nombreVotante.isBlank()) ? nombreVotante : "Votante";
                String cargoMostrar = (candidatoCargo != null && !candidatoCargo.isBlank()) ? " (" + candidatoCargo + ")" : "";

                // Version Texto Plano (evita filtros antispam)
                String plainText = "VotaSegura - Comprobante de Voto\n\n"
                        + "¡Hola, " + nombreMostrar + "!\n\n"
                        + "Tu voto fue registrado de manera exitosa y protegida.\n\n"
                        + "DATOS DE TU VOTO:\n"
                        + "------------------------------------\n"
                        + "- Folio de tu voto: " + codigoRecibo + "\n"
                        + "- Elección: " + tituloEleccion + "\n"
                        + "- Grupo: " + nombreGrupo + "\n"
                        + "- Votaste por: " + candidatoNombre + cargoMostrar + "\n"
                        + "- Fecha y Hora: " + fechaFormateada + "\n"
                        + "- Estado: Voto Registrado y Protegido\n\n"
                        + "Tu voto es 100% secreto. Nadie puede ver por quién votaste.\n"
                        + "Guarda tu folio para comprobar que tu voto fue contado.\n\n"
                        + "Equipo VotaSegura";

                // Version HTML enriquecida
                String htmlText = construirHtml(
                        nombreMostrar,
                        tituloEleccion,
                        nombreGrupo,
                        candidatoNombre,
                        cargoMostrar,
                        codigoRecibo,
                        fechaFormateada
                );

                helper.setText(plainText, htmlText);
                mailSender.send(mimeMessage);
                log.info("Comprobante de voto enviado exitosamente por correo a: {}", correoDestinatario);
            } catch (Exception e) {
                log.error("Error al enviar comprobante de voto a {}: {}", correoDestinatario, e.getMessage());
            }
        });
    }

    private String construirHtml(
            String nombreVotante,
            String tituloEleccion,
            String nombreGrupo,
            String candidatoNombre,
            String cargoMostrar,
            String codigoRecibo,
            String fechaFormateada) {

        return "<!DOCTYPE html>"
                + "<html lang=\"es\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
                + "<body style=\"margin:0;padding:0;background-color:#F8F9FD;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;\">"
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#F8F9FD;padding:30px 15px;\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:540px;background-color:#FFFFFF;border-radius:24px;box-shadow:0 12px 30px rgba(108,92,231,0.08);border:1px solid #ECECF4;overflow:hidden;\">"
                
                // Header degradado
                + "<tr><td style=\"background:linear-gradient(135deg, #6C5CE7 0%, #8E78FF 100%);padding:32px 25px;text-align:center;\">"
                + "<div style=\"display:inline-block;background:rgba(255,255,255,0.2);padding:6px 14px;border-radius:20px;margin-bottom:8px;\">"
                + "<span style=\"color:#FFFFFF;font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;\">Comprobante de Votación</span>"
                + "</div>"
                + "<h1 style=\"color:#FFFFFF;margin:0;font-size:24px;font-weight:800;letter-spacing:-0.5px;\">VotaSegura</h1>"
                + "<p style=\"color:rgba(255,255,255,0.9);margin:6px 0 0;font-size:12px;letter-spacing:0.3px;\">Tu participación cuenta de forma segura</p>"
                + "</td></tr>"
                
                // Cuerpo
                + "<tr><td style=\"padding:32px 28px 24px 28px;\">"
                + "<div style=\"text-align:center;margin-bottom:20px;\">"
                + "<span style=\"display:inline-block;background-color:#ECFDF5;color:#059669;border:1px solid #A7F3D0;padding:6px 16px;border-radius:30px;font-size:12px;font-weight:700;\">"
                + "✓ Voto Registrado con Éxito"
                + "</span>"
                + "</div>"
                
                + "<h2 style=\"color:#1E1B2E;margin:0 0 8px;font-size:18px;font-weight:700;text-align:center;\">"
                + "¡Gracias por participar, " + nombreVotante + "!"
                + "</h2>"
                + "<p style=\"color:#64748B;font-size:13px;line-height:1.5;margin:0 0 24px;text-align:center;\">"
                + "Tu voto fue recibido de manera exitosa y protegida. A continuación tienes los datos de tu comprobante:"
                + "</p>"

                // Tarjeta de Recibo
                + "<div style=\"background-color:#F5F3FF;border:2px dashed #6C5CE7;border-radius:16px;padding:20px;text-align:center;margin-bottom:24px;\">"
                + "<div style=\"color:#6C5CE7;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1px;margin-bottom:6px;\">"
                + "Folio de tu voto"
                + "</div>"
                + "<div style=\"font-family:'Courier New',Courier,monospace;font-size:26px;font-weight:800;color:#1E1B2E;letter-spacing:4px;\">"
                + codigoRecibo
                + "</div>"
                + "<p style=\"color:#8B5CF6;font-size:11px;margin:8px 0 0;font-weight:500;\">Guarda este folio para comprobar que tu voto fue contado correctamente.</p>"
                + "</div>"

                // Tabla de Datos de la Elección
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#F8FAFC;border:1px solid #E2E8F0;border-radius:14px;overflow:hidden;margin-bottom:24px;\">"
                + "<tr><td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#64748B;font-size:12px;font-weight:600;width:35%;\">Elección:</td>"
                + "<td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#1E1B2E;font-size:13px;font-weight:700;\">" + tituloEleccion + "</td></tr>"
                
                + "<tr><td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#64748B;font-size:12px;font-weight:600;\">Grupo:</td>"
                + "<td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#1E1B2E;font-size:13px;font-weight:600;\">" + nombreGrupo + "</td></tr>"

                + "<tr><td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#64748B;font-size:12px;font-weight:600;\">Votaste por:</td>"
                + "<td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#6C5CE7;font-size:13px;font-weight:700;\">" + candidatoNombre + "<span style=\"color:#64748B;font-weight:500;\">" + cargoMostrar + "</span></td></tr>"

                + "<tr><td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#64748B;font-size:12px;font-weight:600;\">Fecha y hora:</td>"
                + "<td style=\"padding:12px 16px;border-bottom:1px solid #E2E8F0;color:#334155;font-size:12px;\">" + fechaFormateada + "</td></tr>"

                + "<tr><td style=\"padding:12px 16px;color:#64748B;font-size:12px;font-weight:600;\">Estado:</td>"
                + "<td style=\"padding:12px 16px;color:#059669;font-size:12px;font-weight:600;\">Voto Registrado y Protegido ✓</td></tr>"
                + "</table>"

                // Cuadro informativo de privacidad
                + "<div style=\"background-color:#F0FDF4;border-left:4px solid #10B981;padding:12px 14px;border-radius:8px;margin-bottom:20px;\">"
                + "<p style=\"color:#065F46;font-size:11px;line-height:1.5;margin:0;\">"
                + "<strong>Tu voto es 100% secreto:</strong> Nadie puede ver por quién votaste. Tu nombre y tu voto se guardan por separado para garantizar tu total privacidad."
                + "</p></div>"

                + "<p style=\"color:#94A3B8;font-size:11px;text-align:center;margin:0;line-height:1.4;\">Este comprobante se generó automáticamente al confirmar tu voto en VotaSegura.</p>"
                + "</td></tr>"

                // Footer
                + "<tr><td style=\"background-color:#F8F9FD;padding:18px 25px;text-align:center;border-top:1px solid #ECECF4;\">"
                + "<p style=\"color:#94A3B8;font-size:11px;margin:0;\">© 2026 VotaSegura · Votaciones seguras, privadas y transparentes</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }
}
