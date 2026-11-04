package com.votasegura.api.repository;

import com.votasegura.api.model.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidatoRepository extends JpaRepository<Candidato, String> {
    List<Candidato> findByEleccionIdOrderByVotosDesc(String eleccionId);
    void deleteByEleccionId(String eleccionId);
}