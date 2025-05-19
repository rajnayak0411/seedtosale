package com.seedtosale;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SeedtosaleApplication {
		
		public static void main(String[] args) {
				// Create upload directories if they don't exist
				createUploadDirectories();
				
				SpringApplication.run(SeedtosaleApplication.class, args);
		}
		
		private static void createUploadDirectories() {
				try {
						Path seedsPath = Paths.get("uploads/seeds");
						Path tractorsPath = Paths.get("uploads/tractors");
						
						if (!Files.exists(seedsPath)) {
								Files.createDirectories(seedsPath);
						}
						
						if (!Files.exists(tractorsPath)) {
								Files.createDirectories(tractorsPath);
						}
				} catch (IOException e) {
						e.printStackTrace();
				}
		}
}