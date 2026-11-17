package com.bookdhara.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPhoneOrderByCreatedAtDesc(String phone);

    long countByPhoneAndCreatedAtAfter(String phone, Instant after);
}
