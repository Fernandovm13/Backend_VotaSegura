package com.votasegura.api.repository;

import com.votasegura.api.model.DesafioChallenge;
import com.votasegura.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DesafioChallengeRepository extends JpaRepository<DesafioChallenge, String> {
    Optional<DesafioChallenge> findFirstByUsuarioAndTipoAndUsadoFalseAndExpiraEnAfterOrderByCreadoEnDesc(
        Usuario usuario, String tipo, LocalDateTime now);

    Optional<DesafioChallenge> findByChallengeAndTipoAndUsadoFalseAndExpiraEnAfter(
        String challenge, String tipo, LocalDateTime now);

    List<DesafioChallenge> findAllByUsuarioAndTipoAndUsadoFalse(Usuario usuario, String tipo);
}
