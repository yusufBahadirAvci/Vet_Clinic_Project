package com.vetclinic.auth.repository;

import com.vetclinic.auth.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    List<OtpCode> findAllByUserIdAndUsedAtIsNull(Long userId);

    Optional<OtpCode> findTopByDestinationOrderByCreatedAtDesc(String destination);

    /** Rate limiting: how many codes were sent to this destination after the given time. */
    long countByDestinationAndCreatedAtAfter(String destination, OffsetDateTime after);
}
