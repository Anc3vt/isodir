package com.ancevt.util.isodir;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;

public class IsolatedDirectory {

    private final Path base;

    public IsolatedDirectory(Path basePath) {
        this.base = Objects.requireNonNull(basePath).toAbsolutePath().normalize();
    }

    public IsolatedDirectory(String basePath) {
        this(Path.of(basePath));
    }

    public Path base() {
        if (!Files.exists(base)) {
            try {
                Files.createDirectories(base);
            } catch (IOException e) {
                throw new IsolatedDirectoryException("Could not create baseDir: " + base, e);
            }
        }
        return base;
    }

    public Path resolve(String relativePath) {
        Path resolved = base();
        for (String token : relativePath.split("/")) {
            if (!token.isEmpty()) resolved = resolved.resolve(token);
        }
        Path normalized = resolved.normalize();

        if (!normalized.startsWith(base)) {
            throw new IsolatedDirectoryException("Path traversal attempt: " + relativePath);
        }

        return normalized;
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    public Path createDir(String relativePath) {
        Path path = resolve(relativePath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not create subdirectories: " + relativePath, e);
        }
        return path;
    }

    public void delete(String relativePath) {
        Path path = resolve(relativePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not delete: " + relativePath, e);
        }
    }

    public void deleteDir(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.exists(path)) return;

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not recursively delete directory: " + relativePath, e);
        }
    }

    public void writeBytes(String relativePath, byte[] bytes) {
        try {
            Files.write(resolve(relativePath), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not write bytes: " + relativePath, e);
        }
    }

    public void appendBytes(String relativePath, byte[] data) {
        try {
            Files.write(resolve(relativePath), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not append bytes: " + relativePath, e);
        }
    }

    public void writeText(String relativePath, String text, Charset charset) {
        writeBytes(relativePath, text.getBytes(charset));
    }

    public void writeText(String relativePath, String text) {
        writeText(relativePath, text, StandardCharsets.UTF_8);
    }

    public void appendText(String relativePath, String text, Charset charset) {
        appendBytes(relativePath, text.getBytes(charset));
    }

    public void appendText(String relativePath, String text) {
        appendText(relativePath, text, StandardCharsets.UTF_8);
    }

    public byte[] readBytes(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not read bytes: " + relativePath, e);
        }
    }

    public String readText(String relativePath, Charset charset) {
        return new String(readBytes(relativePath), charset);
    }

    public String readText(String relativePath) {
        return readText(relativePath, StandardCharsets.UTF_8);
    }

    public InputStream read(String relativePath) {
        try {
            return Files.newInputStream(resolve(relativePath));
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not open input stream: " + relativePath, e);
        }
    }

    public OutputStream createOutputStream(String relativePath, boolean overwrite) {
        try {
            return Files.newOutputStream(
                    resolve(relativePath),
                    StandardOpenOption.CREATE,
                    overwrite ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not open output stream: " + relativePath, e);
        }
    }

    public OutputStream createOutputStream(String relativePath) {
        return createOutputStream(relativePath, true);
    }

    public static IsolatedDirectory getLocal(Path relative) {
        return new IsolatedDirectory(OsUtils.getApplicationDataPath().resolve(relative));
    }

    public static IsolatedDirectory getLocal(String relative) {
        return getLocal(Path.of(relative));
    }

    @Override
    public String toString() {
        return "IsolatedDirectory{baseDir=" + base + '}';
    }
}
