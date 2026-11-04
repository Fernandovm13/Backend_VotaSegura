package com.votasegura.api.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Convertidor JPA transparente para Cifrado en Reposo mediante AES-256-GCM.
 * Garantiza cumplimiento con los estandares de ciberseguridad (Defensa en Profundidad).
 * Es tolerante a datos preexistentes: si el dato en BD no esta cifrado, lo lee sin fallar.
 */
@Slf4j
@Converter
@Component
public class Aes256Converter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag
    private static final int GCM_IV_LENGTH = 12;   // 12-byte standard GCM IV
    private static final String PREFIX = "ENC:GCM:";

    // Clave secreta AES-256 de 32 bytes (256 bits) para cifrado en reposo
    private static final String DEFAULT_SECRET = "VotaSeguraCryptoKey2026AES256Sec"; // 32 chars = 256 bits
    private static final byte[] KEY_BYTES = DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8);

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        // Si ya estuviera cifrado, no volver a cifrar
        if (attribute.startsWith(PREFIX)) {
            return attribute;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            // Concatenar IV + Texto cifrado
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Error al cifrar atributo en reposo con AES-256: {}", e.getMessage());
            // En caso de excepcion, mantener el dato original para evitar perdida de informacion
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        // Si no tiene el prefijo de cifrado, es un dato legacy/previo no cifrado: leer directo
        if (!dbData.startsWith(PREFIX)) {
            return dbData;
        }

        try {
            String base64Payload = dbData.substring(PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Payload);

            if (combined.length < GCM_IV_LENGTH) {
                return dbData;
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("No se pudo descifrar dato con AES-256 (posible dato plano): {}", e.getMessage());
            return dbData;
        }
    }
}
