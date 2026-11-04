package com.votasegura.api.repository;

import com.votasegura.api.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {
    boolean existsByToken(String token);
    void deleteByExpiraEnBefore(LocalDateTime now);
}
