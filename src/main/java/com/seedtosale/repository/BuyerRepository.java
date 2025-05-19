package com.seedtosale.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seedtosale.dto.SignupDto;


public interface BuyerRepository extends JpaRepository<SignupDto, Long> {
    SignupDto findByEmail(String email); // Method to find a buyer by email
}
