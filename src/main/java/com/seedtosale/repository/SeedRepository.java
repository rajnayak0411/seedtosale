package com.seedtosale.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.seedtosale.model.Seed;

import java.util.List;

@Repository
public interface SeedRepository extends JpaRepository<Seed, Long> {
    List<Seed> findByTypeContainingIgnoreCaseAndLocationContainingIgnoreCaseAndPriceLessThanEqualAndNameContainingIgnoreCase(
        String type, String location, double price, String name);

    // Custom method to get latest seeds ordered by id descending with limit
    @org.springframework.data.jpa.repository.Query(value = "SELECT s FROM Seed s ORDER BY s.id DESC")
    java.util.List<Seed> findLatestSeeds(org.springframework.data.domain.Pageable pageable);
}
