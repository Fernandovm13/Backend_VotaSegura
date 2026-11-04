package com.votasegura.api.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class OtpGenerador {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Genera un codigo OTP de 6 digitos numericos usando SecureRandom
     * para garantizar aleatoriedad criptograficamente segura.
     */
    public String generar() {
        int codigo = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(codigo);
    }
}
