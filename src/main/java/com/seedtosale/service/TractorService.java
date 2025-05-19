package com.seedtosale.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.seedtosale.model.Tractor;
import com.seedtosale.repository.TractorRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class TractorService {

    @Autowired
    private final TractorRepository tractorRepository;
    private final String uploadDir = "uploads/tractors/";

    public TractorService(TractorRepository tractorRepository) {
        this.tractorRepository = tractorRepository;
    }

    public Tractor saveTractor(Tractor tractor, MultipartFile photo) throws IOException {
        // Save photo to file system
        if (photo != null && !photo.isEmpty()) {
            byte[] bytes = photo.getBytes();
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);
            Path path = dirPath.resolve(photo.getOriginalFilename());
            Files.write(path, bytes);
            tractor.setImage(photo.getOriginalFilename());
        }
        // Save tractor entity to database
        return tractorRepository.save(tractor);
    }

    public java.util.List<Tractor> getFilteredTractors(String location, double maxPrice, String search) {
        return tractorRepository.findByLocationContainingIgnoreCaseAndPriceLessThanEqualAndNameContainingIgnoreCase(
            location, maxPrice, search);
    }

    public java.util.List<Tractor> getAllTractors() {
        return tractorRepository.findAll();
    }

    public Tractor getTractorById(Long id) {
        return tractorRepository.findById(id).orElse(null);
    }

    public Tractor updateTractor(Long id, Tractor updatedTractor, MultipartFile photo) throws IOException {
        Tractor existingTractor = getTractorById(id);
        if (existingTractor == null) {
            return null;
        }
        existingTractor.setName(updatedTractor.getName());
        existingTractor.setLocation(updatedTractor.getLocation());
        existingTractor.setPrice(updatedTractor.getPrice());
        existingTractor.setDescription(updatedTractor.getDescription());
        existingTractor.setStockQuantity(updatedTractor.getStockQuantity());
        existingTractor.setSoldQuantity(updatedTractor.getSoldQuantity());
        
      
        if (photo != null && !photo.isEmpty()) {
            byte[] bytes = photo.getBytes();
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);
            Path path = dirPath.resolve(photo.getOriginalFilename());
            Files.write(path, bytes);
            existingTractor.setImage(photo.getOriginalFilename());
        }

        return tractorRepository.save(existingTractor);
    }

    public void deleteTractor(Long id) {
        tractorRepository.deleteById(id);
    }

    // New method to get latest tractors
    public java.util.List<Tractor> getLatestTractors(int count) {
        return tractorRepository.findLatestTractors(org.springframework.data.domain.PageRequest.of(0, count));
    }
}
