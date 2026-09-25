package org.roadwatch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ImageStorageService {
    private final Path storageDirectory;
    private final String supabaseUrl;
    private final String supabaseBucket;
    private final String supabaseKey;
    private final boolean production;
    private final RestClient client;
    public ImageStorageService(@Value("${app.storage.directory:./uploads}") String directory,
                               @Value("${app.supabase.url:}") String supabaseUrl,
                               @Value("${app.supabase.bucket:roadwatch-images}") String supabaseBucket,
                               @Value("${app.supabase.service-key:}") String supabaseKey,
                               @Value("${app.environment:local}") String environment) {
        this.storageDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.replaceAll("/$", "");
        this.supabaseBucket = supabaseBucket;
        this.supabaseKey = supabaseKey == null ? "" : supabaseKey;
        this.production = "production".equalsIgnoreCase(environment);
        if (production && (this.supabaseUrl.isBlank() || this.supabaseKey.isBlank()))
            throw new IllegalStateException("Production mode requires Supabase Storage credentials; local disk is not durable on free hosts");
        if (this.supabaseUrl.isBlank() != this.supabaseKey.isBlank())
            throw new IllegalStateException("Set both SUPABASE_URL and SUPABASE_SERVICE_KEY, or leave both empty for local development");
        this.client = RestClient.create();
    }

    public String store(MultipartFile image) {
        if (image == null || image.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a road photo first");
        String type = image.getContentType();
        String extension = "image/jpeg".equalsIgnoreCase(type) ? ".jpg" : "image/png".equalsIgnoreCase(type) ? ".png" : null;
        if (extension == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a JPEG or PNG image");
        if (image.getSize() > 8L * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Images must be 8 MB or smaller");
        try {
            byte[] signature;
            try (var input = image.getInputStream()) { signature = input.readNBytes(8); }
            boolean jpeg = signature.length >= 3 && (signature[0] & 0xff) == 0xff && (signature[1] & 0xff) == 0xd8 && (signature[2] & 0xff) == 0xff;
            boolean png = signature.length == 8 && signature[0] == (byte)0x89 && signature[1] == 0x50 && signature[2] == 0x4e && signature[3] == 0x47;
            if ((extension.equals(".jpg") && !jpeg) || (extension.equals(".png") && !png))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file contents do not match a JPEG or PNG image");
            try (var imageInput = ImageIO.createImageInputStream(image.getInputStream())) {
                var readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected image is damaged or unreadable");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || width > 12_000 || height > 12_000 || (long) width * height > 40_000_000L)
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The image dimensions are too large");
                } finally { reader.dispose(); }
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the selected image", exception);
        }
        String name = UUID.randomUUID() + extension;
        try {
            if (!supabaseUrl.isBlank()) {
                byte[] bytes = image.getBytes();
                client.post().uri(supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + name)
                        .header("apikey", supabaseKey)
                        .header("Content-Type", type)
                        .body(bytes).retrieve().toBodilessEntity();
                return supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + name;
            }
            Files.createDirectories(storageDirectory);
            Path destination = storageDirectory.resolve(name).normalize();
            if (!destination.startsWith(storageDirectory)) throw new IOException("Invalid upload path");
            image.transferTo(destination);
            return "/uploads/" + name;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The image could not be saved", exception);
        }
    }
}
