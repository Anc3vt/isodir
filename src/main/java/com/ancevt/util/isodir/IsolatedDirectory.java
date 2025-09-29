package com.ancevt.util.isodir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Provides a sandboxed directory for safe file I/O operations.
 * <p>
 * All paths are resolved relative to a predefined base directory and
 * path traversal outside the base is blocked.
 * </p>
 *
 * <p>Example:</p>
 * <pre>
 *     IsolatedDirectory dir = new IsolatedDirectory("data");
 *     dir.writeText("config/settings.txt", "volume=80");
 *     String text = dir.readText("config/settings.txt");
 * </pre>
 */
public class IsolatedDirectory {

    private final Path base;

    /**
     * Creates a new isolated directory rooted at the given base {@link Path}.
     * The base path is normalized and will be created if it doesn't exist.
     *
     * @param basePath base directory path
     */
    public IsolatedDirectory(Path basePath) {
        this.base = Objects.requireNonNull(basePath).toAbsolutePath().normalize();
    }

    /**
     * Creates a new isolated directory rooted at the given base path string.
     *
     * @param basePath base directory path as string
     */
    public IsolatedDirectory(String basePath) {
        this(Path.of(basePath));
    }

    /**
     * Returns the base path of this isolated directory.
     * If it does not exist yet, it will be created.
     *
     * @return base directory path
     * @throws IsolatedDirectoryException if base cannot be created
     */
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

    /**
     * Resolves a relative path within the base directory.
     * Ensures no path traversal outside of the base is possible.
     *
     * @param relativePath relative path inside the base
     * @return resolved {@link Path} inside the base
     * @throws IsolatedDirectoryException if path escapes base
     */
    public Path resolve(String relativePath) {
        Path resolved = base();

        for (String token : relativePath.split("/")) {
            if (token.isEmpty() || token.equals(".")) continue;
            if (token.equals("..")) {
                throw new IsolatedDirectoryException("Path traversal attempt: " + relativePath);
            }

            resolved = resolved.resolve(token);

            if (Files.isSymbolicLink(resolved)) {
                throw new IsolatedDirectoryException("Symlink detected in path: " + resolved);
            }
        }

        Path normalized = resolved.normalize();

        if (!normalized.startsWith(base)) {
            throw new IsolatedDirectoryException("Resolved path escapes base: " + relativePath);
        }

        return normalized;
    }


    /**
     * Checks if a file or directory exists inside the base.
     *
     * @param relativePath relative path inside the base
     * @return true if the path exists
     */
    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    /**
     * Creates a directory (including intermediate directories) inside the base.
     *
     * @param relativePath relative directory path
     * @return created directory {@link Path}
     * @throws IsolatedDirectoryException if creation fails
     */
    public Path createDir(String relativePath) {
        Path path = resolve(relativePath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not create subdirectories: " + relativePath, e);
        }
        return path;
    }

    /**
     * Deletes a single file or empty directory inside the base if it exists.
     *
     * @param relativePath relative path to delete
     * @throws IsolatedDirectoryException if deletion fails
     */
    public void delete(String relativePath) {
        Path path = resolve(relativePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not delete: " + relativePath, e);
        }
    }

    /**
     * Recursively deletes a directory and its contents inside the base.
     * If the directory does not exist, nothing happens.
     *
     * @param relativePath relative directory path to delete
     * @throws IsolatedDirectoryException if deletion fails
     */
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

    /**
     * Writes bytes to a file inside the base, creating it if necessary.
     * Overwrites any existing content.
     *
     * @param relativePath relative file path
     * @param bytes        bytes to write
     * @throws IsolatedDirectoryException if writing fails
     */
    public void writeBytes(String relativePath, byte[] bytes) {
        try {
            Files.write(resolve(relativePath), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not write bytes: " + relativePath, e);
        }
    }

    /**
     * Appends bytes to a file inside the base, creating it if necessary.
     *
     * @param relativePath relative file path
     * @param data         bytes to append
     * @throws IsolatedDirectoryException if writing fails
     */
    public void appendBytes(String relativePath, byte[] data) {
        try {
            Files.write(resolve(relativePath), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not append bytes: " + relativePath, e);
        }
    }

    /**
     * Writes text to a file inside the base using the given charset.
     * Overwrites any existing content.
     *
     * @param relativePath relative file path
     * @param text         text to write
     * @param charset      charset to use
     */
    public void writeText(String relativePath, String text, Charset charset) {
        writeBytes(relativePath, text.getBytes(charset));
    }

    /**
     * Writes text to a file inside the base using UTF-8.
     *
     * @param relativePath relative file path
     * @param text         text to write
     */
    public void writeText(String relativePath, String text) {
        writeText(relativePath, text, StandardCharsets.UTF_8);
    }

    /**
     * Appends text to a file inside the base using the given charset.
     *
     * @param relativePath relative file path
     * @param text         text to append
     * @param charset      charset to use
     */
    public void appendText(String relativePath, String text, Charset charset) {
        appendBytes(relativePath, text.getBytes(charset));
    }

    /**
     * Appends text to a file inside the base using UTF-8.
     *
     * @param relativePath relative file path
     * @param text         text to append
     */
    public void appendText(String relativePath, String text) {
        appendText(relativePath, text, StandardCharsets.UTF_8);
    }

    /**
     * Reads all bytes from a file inside the base.
     *
     * @param relativePath relative file path
     * @return file contents as byte array
     * @throws IsolatedDirectoryException if reading fails
     */
    public byte[] readBytes(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not read bytes: " + relativePath, e);
        }
    }

    /**
     * Reads text from a file inside the base using the given charset.
     *
     * @param relativePath relative file path
     * @param charset      charset to use
     * @return file contents as string
     */
    public String readText(String relativePath, Charset charset) {
        return new String(readBytes(relativePath), charset);
    }

    /**
     * Reads text from a file inside the base using UTF-8.
     *
     * @param relativePath relative file path
     * @return file contents as string
     */
    public String readText(String relativePath) {
        return readText(relativePath, StandardCharsets.UTF_8);
    }

    /**
     * Opens an {@link InputStream} to read a file inside the base.
     * The caller is responsible for closing the stream.
     *
     * @param relativePath relative file path
     * @return input stream for reading
     * @throws IsolatedDirectoryException if opening fails
     */
    public InputStream read(String relativePath) {
        try {
            return Files.newInputStream(resolve(relativePath));
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not open input stream: " + relativePath, e);
        }
    }

    /**
     * Opens an {@link OutputStream} to write to a file inside the base.
     *
     * @param relativePath relative file path
     * @param overwrite    true to overwrite existing file, false to append
     * @return output stream for writing
     * @throws IsolatedDirectoryException if opening fails
     */
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

    /**
     * Opens an {@link OutputStream} to write to a file inside the base, overwriting it by default.
     *
     * @param relativePath relative file path
     * @return output stream for writing
     */
    public OutputStream createOutputStream(String relativePath) {
        return createOutputStream(relativePath, true);
    }

    /**
     * Creates an isolated directory under the OS-specific application data path.
     *
     * @param relative relative path inside the application data folder
     * @return new {@link IsolatedDirectory} instance
     */
    public static IsolatedDirectory getLocal(Path relative) {
        return new IsolatedDirectory(OsUtils.getApplicationDataPath().resolve(relative));
    }

    /**
     * Creates an isolated directory under the OS-specific application data path.
     *
     * @param relative relative path string inside the application data folder
     * @return new {@link IsolatedDirectory} instance
     */
    public static IsolatedDirectory getLocal(String relative) {
        return getLocal(Path.of(relative));
    }

    @Override
    public String toString() {
        return "IsolatedDirectory{baseDir=" + base + '}';
    }
}
