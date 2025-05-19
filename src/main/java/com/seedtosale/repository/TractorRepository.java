package com.seedtosale.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.seedtosale.model.Tractor;

@Repository
public interface TractorRepository extends JpaRepository<Tractor, Long> {
    java.util.List<Tractor> findByLocationContainingIgnoreCaseAndPriceLessThanEqualAndNameContainingIgnoreCase(
        String location, double price, String name);

    // Custom method to get latest tractors ordered by id descending with limit
    @org.springframework.data.jpa.repository.Query(value = "SELECT t FROM Tractor t ORDER BY t.id DESC")
    java.util.List<Tractor> findLatestTractors(org.springframework.data.domain.Pageable pageable);
}
