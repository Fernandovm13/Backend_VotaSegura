package com.votasegura.api.repository;

import com.votasegura.api.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, String> {
    Optional<Grupo> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}