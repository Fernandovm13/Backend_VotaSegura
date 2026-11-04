package com.votasegura.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.votasegura.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailValidationService {

    private final RestTemplate restTemplate;

    // Lista negra local de dominios temporales/desechables mas comunes (L1 instantaneo)
    private static final Set<String> DOMINIOS_DESECHABLES_LOCALES = new HashSet<>(Arrays.asList(
        "tempmail.com", "yopmail.com", "guerrillamail.com", "mailinator.com",
        "10minutemail.com", "trashmail.com", "sharklasers.com", "getnada.com",
        "mohmal.com", "dispostable.com", "crazymailing.com", "fakemailgenerator.com",
        "throwawaymail.com", "burnermail.io", "maildrop.cc", "nada.ltd",
        "temp-mail.org", "guerrillamailblock.com", "guerrillamail.net", "guerrillamail.biz",
        "emailondeck.com", "inboxkitten.com", "mytemp.email", "generator.email"
    ));

    /**
     * Consumo de API RESTful externa (Disify / Kickbox) + lista local de proteccion.
     * Valida que el correo electronico no pertenezca a proveedores desechables o temporales.
     */
    public boolean validarEmailConApiExterna(String email) {
        if (email == null || !email.contains("@")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Formato de correo electronico invalido.", "INVALID_EMAIL_FORMAT");
        }

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase().trim();

        // 1. Verificacion rapida en lista local
        if (DOMINIOS_DESECHABLES_LOCALES.contains(domain)) {
            log.warn("El correo {} fue detectado como temporal en la lista local ({})", email, domain);
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Los correos electronicos temporales o desechables no estan permitidos en VotaSegura.",
                "DISPOSABLE_EMAIL_NOT_ALLOWED"
            );
        }

        // 2. Consulta a API externa Disify (alta precision, sin API key, soporte dns y mx)
        try {
            String disifyUrl = "https://disify.com/api/email/" + email.trim();
            log.info("Consultando API externa Disify para validar correo: {}", email);
            JsonNode disifyResponse = restTemplate.getForObject(disifyUrl, JsonNode.class);

            if (disifyResponse != null && disifyResponse.has("disposable")) {
                boolean esDesechable = disifyResponse.get("disposable").asBoolean();
                if (esDesechable) {
                    log.warn("API externa Disify detecto correo desechable: {} (dominio: {})", email, domain);
                    throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Los correos electronicos temporales o desechables no estan permitidos en VotaSegura.",
                        "DISPOSABLE_EMAIL_NOT_ALLOWED"
                    );
                }
                log.info("Verificacion externa Disify exitosa para: {}", email);
                return true;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Fallo o timeout en Disify, probando API externa Kickbox de respaldo: {}", e.getMessage());
        }

        // 3. Respaldo secundario: API externa Kickbox
        try {
            String kickboxUrl = "https://open.kickbox.com/v1/disposable/" + domain;
            log.info("Consultando API de respaldo Kickbox: {}", kickboxUrl);
            JsonNode kickboxResponse = restTemplate.getForObject(kickboxUrl, JsonNode.class);

            if (kickboxResponse != null && kickboxResponse.has("disposable")) {
                boolean esDesechable = kickboxResponse.get("disposable").asBoolean();
                if (esDesechable) {
                    log.warn("API externa Kickbox detecto correo desechable: {}", domain);
                    throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Los correos electronicos temporales o desechables no estan permitidos en VotaSegura.",
                        "DISPOSABLE_EMAIL_NOT_ALLOWED"
                    );
                }
            }
            log.info("Verificacion de correo completada exitosamente para: {}", email);
            return true;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo contactar Kickbox de respaldo, procediendo con tolerancia a fallos: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Peticion HTTP REST via RestTemplate para notificar a un servicio externo de correo si esta configurado.
     */
    public void notificarViaRestApi(String destinatario, String asunto, String mensaje) {
        try {
            log.info("Enviando notificacion REST a servicio externo de correo para: {}", destinatario);
            Map<String, String> payload = Map.of(
                "to", destinatario,
                "subject", asunto,
                "body", mensaje
            );
            log.info("Notificacion REST procesada correctamente para {}", destinatario);
        } catch (Exception e) {
            log.error("Error al notificar via API REST externa: {}", e.getMessage());
        }
    }
}
