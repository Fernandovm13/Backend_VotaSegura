package com.votasegura.api.repository;

import com.votasegura.api.model.Eleccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EleccionRepository extends JpaRepository<Eleccion, String> {
    List<Eleccion> findByGrupoIdOrderByCreadoEnDesc(String grupoId);
    long countByGrupoId(String grupoId);
    List<Eleccion> findByEstadoAndFechaFinBefore(String estado, LocalDateTime fechaFin);
    List<Eleccion> findByEstadoAndFechaInicioBefore(String estado, LocalDateTime fechaInicio);
    void deleteByGrupoId(String grupoId);
}