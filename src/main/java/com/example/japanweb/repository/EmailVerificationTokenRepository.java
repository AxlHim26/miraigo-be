package com.example.japanweb.repository;

import com.example.japanweb.entity.EmailVerificationToken;
import com.example.japanweb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    void deleteByUserAndUsedAtIsNull(User user);
}
