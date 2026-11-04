package com.votasegura.api.repository;

import com.votasegura.api.model.MiembroGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiembroGrupoRepository extends JpaRepository<MiembroGrupo, String> {
    List<MiembroGrupo> findByUsuarioId(String usuarioId);
    Optional<MiembroGrupo> findByGrupoIdAndUsuarioId(String grupoId, String usuarioId);
    boolean existsByGrupoIdAndUsuarioId(String grupoId, String usuarioId);
    long countByGrupoId(String grupoId);

    @Query("SELECT COUNT(m) FROM MiembroGrupo m WHERE m.grupo.id = :grupoId AND LOWER(m.usuario.estado) NOT IN ('inactivo', 'desactivado', 'bloqueado')")
    long countActivosByGrupoId(@Param("grupoId") String grupoId);

    void deleteByGrupoId(String grupoId);
}
