package com.bookdhara.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByEmailOrderByCreatedAtDesc(String email);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM OtpCode c WHERE c.email = :email")
    int deleteByEmail(@org.springframework.data.repository.query.Param("email") String email);

    long countByEmailAndCreatedAtAfter(String email, Instant after);
}
