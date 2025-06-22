package com.ancevt.util.isodir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.StringTokenizer;

public class IsolatedDirectory {

    private final Path base;

    public IsolatedDirectory(Path basePath) {
        this.base = basePath;
    }

    public IsolatedDirectory(String basePath) {
        this(Path.of(basePath));
    }

    public Path base() {
        if (!Files.exists(base)) {
            try {
                Files.createDirectories(base);
            } catch (IOException e) {
                throw new IsolatedDirectoryException("Could not create baseDir", e);
            }
        }

        return base;
    }

    public Path resolve(String relativePath) {
        StringTokenizer stringTokenizer = new StringTokenizer(relativePath, "/");

        Path p = base();

        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            if (!token.isEmpty()) {
                p = p.resolve(token);
            }
        }

        return p;
    }

    public void delete(String relativePath) {
        Path path = resolve(relativePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not delete", e);
        }
    }

    public void deleteDir(String relativePath) {
        Path path = resolve(relativePath);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not delete recursively", e);
        }
    }

    public Path createDir(String relativePath) {
        Path path = base();

        StringTokenizer stringTokenizer = new StringTokenizer(relativePath, "/");

        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            if (!token.isEmpty()) {
                path = path.resolve(token);
            }
        }

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new IsolatedDirectoryException("Could not create subdirectories", e);
            }
        }

        return path;
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    public InputStream read(String relativePath) {
        try {
            return Files.newInputStream(resolve(relativePath));
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not read", e);
        }
    }

    public String readText(String relativePath) {
        return readText(relativePath, "UTF-8");
    }

    public String readText(String relativePath, String charsetName) {
        try {
            return new String(read(relativePath).readAllBytes(), charsetName);
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not read text", e);
        }
    }

    public byte[] readBytes(String relativePath) {
        try {
            return read(relativePath).readAllBytes();
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not read bytes", e);
        }
    }

    public void appendBytes(String relativePath, byte[] data) {
        try {
            Files.write(
                    resolve(relativePath),
                    data,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not write bytes", e);

        }
    }

    public void appendText(String relativePath, String text, String charsetName) {
        try {
            appendBytes(relativePath, text.getBytes(charsetName));
        } catch (UnsupportedEncodingException e) {
            throw new IsolatedDirectoryException("Charset not supported", e);
        }
    }

    public void appendText(String relativePath, String text) {
        appendText(relativePath, text, "UTF-8");
    }

    public void writeBytes(String relativePath, byte[] bytes) {
        try {
            Files.write(
                    resolve(relativePath),
                    bytes,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not write bytes", e);
        }
    }

    public void writeText(String relativePath, String text, String charsetName) {
        try {
            writeBytes(relativePath, text.getBytes(charsetName));
        } catch (UnsupportedEncodingException e) {
            throw new IsolatedDirectoryException("Charset not supported", e);
        }
    }

    public void writeText(String relativePath, String text) {
        writeText(relativePath, text, "UTF-8");
    }

    public OutputStream createOutputStream(String relativePath) {
        try {
            return Files.newOutputStream(resolve(relativePath));
        } catch (IOException e) {
            throw new IsolatedDirectoryException("Could not write", e);
        }
    }

    public static IsolatedDirectory getLocal(Path path) {
        return new IsolatedDirectory(OsUtils.getApplicationDataPath().resolve(path));
    }

    public static IsolatedDirectory getLocal(String path) {
        return getLocal(Path.of(path));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                "{" +
                "baseDir=" +
                base +
                '}';
    }


}
