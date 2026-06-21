package com.cronitor.repository;

import com.cronitor.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHashAndIsActiveTrue(String keyHash);

    List<ApiKey> findAllByUserIdAndIsActiveTrue(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE ApiKey k SET k.lastUsedAt = :now WHERE k.id = :id")
    void updateLastUsed(@Param("id") UUID id, @Param("now") Instant now);

    default void updateLastUsed(UUID id) {
        updateLastUsed(id, Instant.now());
    }
}
