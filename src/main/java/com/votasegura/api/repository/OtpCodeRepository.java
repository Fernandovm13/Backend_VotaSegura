package com.votasegura.api.repository;

import com.votasegura.api.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    // Solo necesitamos buscar por ID - el resto se valida en Java
    boolean existsByDestinatarioAndPropositoAndExpiraEpochGreaterThan(
        String destinatario, String proposito, long expiraEpoch);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiraEpoch < :ahoraEpoch")
    void deleteByExpiraEpochBefore(long ahoraEpoch);
}