package com.votasegura.api.repository;

import com.votasegura.api.model.CredencialBiometrica;
import com.votasegura.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CredencialBiometricaRepository extends JpaRepository<CredencialBiometrica, String> {
    List<CredencialBiometrica> findByUsuario(Usuario usuario);
    Optional<CredencialBiometrica> findByCredentialId(String credentialId);
    boolean existsByCredentialId(String credentialId);
}
