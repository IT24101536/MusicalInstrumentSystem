// FileStorageService.java
package org.example.musicalinstrumentsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService() {
        // Create uploads directory in the project root
        this.uploadDir = Paths.get("uploads/products");
        try {
            Files.createDirectories(uploadDir);
            System.out.println("✅ Upload directory created/verified: " + uploadDir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Only image files are allowed!");
        }

        // Validate file size
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IOException("File size must not exceed 5MB!");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Path targetLocation = uploadDir.resolve(uniqueFilename);

        // Save file
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        System.out.println("✅ File saved: " + uniqueFilename);
        
        // Return relative path for web access
        return "/uploads/products/" + uniqueFilename;
    }


    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty() || !fileUrl.startsWith("/uploads/products/")) {
            return; // Not a local file, skip deletion
        }

        try {
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = uploadDir.resolve(filename);
            Files.deleteIfExists(filePath);
            System.out.println("✅ File deleted: " + filename);
        } catch (IOException e) {
            System.err.println("❌ Failed to delete file: " + fileUrl);
            e.printStackTrace();
        }
    }
}
