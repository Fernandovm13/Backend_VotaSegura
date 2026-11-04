package com.votasegura.api.config;

import com.votasegura.api.model.Eleccion;
import com.votasegura.api.repository.EleccionRepository;
import com.votasegura.api.repository.OtpCodeRepository;
import com.votasegura.api.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupTask {

    private static final ZoneId ZONA_MEXICO = ZoneId.of("America/Mexico_City");

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final EleccionRepository eleccionRepository;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void limpiarTokensExpirados() {
        tokenBlacklistRepository.deleteByExpiraEnBefore(LocalDateTime.now());
        otpCodeRepository.deleteByExpiraEpochBefore(Instant.now().getEpochSecond());
        log.info("Limpieza de tokens y OTPs expirados completada");
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cerrarEleccionesVencidas() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_MEXICO);
        List<Eleccion> vencidas = eleccionRepository.findByEstadoAndFechaFinBefore("activa", ahora);
        if (!vencidas.isEmpty()) {
            for (Eleccion e : vencidas) {
                e.setEstado("cerrada");
                log.info("⏰ Elección '{}' cerrada automáticamente por CleanupTask en BD.", e.getTitulo());
            }
            eleccionRepository.saveAll(vencidas);
        }
    }
}