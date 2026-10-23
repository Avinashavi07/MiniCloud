package com.minicloud.infrastructure.hypervisor;

import com.minicloud.config.MiniCloudProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class IsoCatalog {

    private final MiniCloudProperties properties;

    public IsoCatalog(MiniCloudProperties properties) {
        this.properties = properties;
    }

    public List<IsoImage> list() {
        List<IsoImage> images = new ArrayList<>();
        for (String directory : properties.getHypervisor().getIsoDirectories()) {
            Path dir = Path.of(directory);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.iso")) {
                for (Path iso : stream) {
                    String fileName = iso.getFileName().toString();
                    if (!fileName.matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,120}\\.iso$")) {
                        continue;
                    }
                    images.add(new IsoImage(fileName, iso.toAbsolutePath().toString(), Files.size(iso)));
                }
            } catch (IOException ignored) {
                // Skip unreadable directories rather than inventing images.
            }
        }
        images.sort(Comparator.comparing(IsoImage::fileName));
        return images;
    }

    public Optional<String> resolve(String operatingSystem) {
        if (operatingSystem == null || operatingSystem.isBlank() || "none".equalsIgnoreCase(operatingSystem)) {
            return Optional.empty();
        }
        String needle = operatingSystem.toLowerCase(Locale.ROOT);
        if (needle.startsWith("ubuntu")) {
            needle = "ubuntu";
        }
        String match = needle;
        return list().stream()
                .map(IsoImage::path)
                .filter(path -> path.toLowerCase(Locale.ROOT).contains(match))
                .findFirst();
    }

    public record IsoImage(String fileName, String path, long sizeBytes) {
    }
}
