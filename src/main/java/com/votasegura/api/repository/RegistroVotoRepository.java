package com.votasegura.api.repository;

import com.votasegura.api.model.RegistroVoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroVotoRepository extends JpaRepository<RegistroVoto, String> {
    boolean existsByEleccionIdAndUsuarioId(String eleccionId, String usuarioId);
    long countByEleccionId(String eleccionId);
    Optional<RegistroVoto> findByEleccionIdAndUsuarioId(String eleccionId, String usuarioId);
    void deleteByEleccionId(String eleccionId);

    /** Historial completo de votos de un usuario, cargando relaciones lazy necesarias */
    @Query("SELECT rv FROM RegistroVoto rv " +
           "JOIN FETCH rv.eleccion e " +
           "JOIN FETCH e.grupo g " +
           "WHERE rv.usuario.id = :usuarioId " +
           "ORDER BY rv.emitidoEn DESC")
    List<RegistroVoto> findByUsuarioIdWithDetails(@Param("usuarioId") String usuarioId);

    /** Cuenta cuántas elecciones ha votado un usuario */
    long countByUsuarioId(String usuarioId);
}
