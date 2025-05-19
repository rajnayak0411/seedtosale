package com.seedtosale.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.seedtosale.model.Seed;
import com.seedtosale.repository.SeedRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class SeedService {
    
    @Autowired
    private final SeedRepository seedRepository;
    private final String uploadDir = "uploads/seeds/";

    
    public SeedService(SeedRepository seedRepository) {
        this.seedRepository = seedRepository;
    }

    public Seed saveSeed(Seed seed, MultipartFile photo) throws IOException {
        // Save photo to file system
        if (photo != null && !photo.isEmpty()) {
            byte[] bytes = photo.getBytes();
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);
            Path path = dirPath.resolve(photo.getOriginalFilename());
            Files.write(path, bytes);
            seed.setImage(photo.getOriginalFilename());
        }
        // Save seed entity to database
        return seedRepository.save(seed);
    }

    public List<Seed> getAllSeeds() {
        return seedRepository.findAll();
    }

    public Seed getSeedById(Long id) {
        return seedRepository.findById(id).orElse(null);
    }

    public Seed updateSeed(Long id, Seed updatedSeed, MultipartFile photo) throws IOException {
        Seed existingSeed = getSeedById(id);
        if (existingSeed == null) {
            return null;
        }
        existingSeed.setName(updatedSeed.getName());
        existingSeed.setType(updatedSeed.getType());
        existingSeed.setLocation(updatedSeed.getLocation());
        existingSeed.setPrice(updatedSeed.getPrice());
        existingSeed.setStockQuantity(updatedSeed.getStockQuantity());
        existingSeed.setSoldQuantity(updatedSeed.getSoldQuantity());

        if (photo != null && !photo.isEmpty()) {
            byte[] bytes = photo.getBytes();
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);
            Path path = dirPath.resolve(photo.getOriginalFilename());
            Files.write(path, bytes);
            existingSeed.setImage(photo.getOriginalFilename());
        }

        return seedRepository.save(existingSeed);
    }

    public List<Seed> getFilteredSeeds(String type, String location, double maxPrice, String search) {
        return seedRepository.findByTypeContainingIgnoreCaseAndLocationContainingIgnoreCaseAndPriceLessThanEqualAndNameContainingIgnoreCase(
            type, location, maxPrice, search);
    }

    public void deleteSeed(Long id) {
        seedRepository.deleteById(id);
    }

    // New method to get latest seeds
    public List<Seed> getLatestSeeds(int count) {
        return seedRepository.findLatestSeeds(org.springframework.data.domain.PageRequest.of(0, count));
    }
}
