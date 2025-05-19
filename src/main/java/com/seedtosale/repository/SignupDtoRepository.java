package com.seedtosale.repository;

import com.seedtosale.dto.SignupDto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SignupDtoRepository extends JpaRepository<SignupDto, Long> {
    Optional<SignupDto> findByEmailIgnoreCase(String email);
}
