package com.ancevt.util.isodir;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectoryTest {

    private Path tempDir;
    private Directory dir;

    @BeforeAll
    void setupAll() throws IOException {
        tempDir = Files.createTempDirectory("directory-test-" + UUID.randomUUID());
        dir = new Directory(tempDir);
    }

    @AfterAll
    void cleanupAll() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
    }

    @Test
    void testWriteAndReadText() {
        String relPath = "test.txt";
        String content = "Привет, Directory!";
        dir.writeText(relPath, content);

        String read = dir.readText(relPath);
        assertEquals(content, read);
    }

    @Test
    void testWriteAndReadBytes() {
        String relPath = "binfile.bin";
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        dir.writeBytes(relPath, bytes);

        byte[] read = dir.readBytes(relPath);
        assertArrayEquals(bytes, read);
    }

    @Test
    void testAppendTextAndBytes() {
        String relPath = "append.txt";
        dir.writeText(relPath, "abc");
        dir.appendText(relPath, "def");
        String result = dir.readText(relPath);
        assertEquals("abcdef", result);

        dir.writeBytes(relPath, "123".getBytes(StandardCharsets.UTF_8));
        dir.appendBytes(relPath, "456".getBytes(StandardCharsets.UTF_8));
        String result2 = dir.readText(relPath);
        assertEquals("123456", result2);
    }

    @Test
    void testExistsAndCreateDir() {
        String subDir = "sub/dir";
        assertFalse(dir.exists(subDir));
        dir.createDir(subDir);
        assertTrue(dir.exists(subDir));
    }

    @Test
    void testDeleteFile() {
        String relPath = "deleteMe.txt";
        dir.writeText(relPath, "to delete");
        assertTrue(dir.exists(relPath));
        dir.delete(relPath);
        assertFalse(dir.exists(relPath));
    }

    @Test
    void testDeleteDir() {
        String relDir = "folder/to/delete";
        dir.createDir(relDir);
        dir.writeText(relDir + "/file.txt", "data");
        assertTrue(dir.exists(relDir + "/file.txt"));
        dir.deleteDir("folder");
        assertFalse(dir.exists(relDir + "/file.txt"));
        assertFalse(dir.exists("folder"));
    }

    @Test
    void testReadNonexistentThrows() {
        String relPath = "no_such_file.txt";
        assertThrows(DirectoryException.class, () -> dir.readText(relPath));
        assertThrows(DirectoryException.class, () -> dir.readBytes(relPath));
        assertThrows(DirectoryException.class, () -> dir.read(relPath));
    }

    @Test
    void testToStringWorks() {
        assertNotNull(dir.toString());
        assertTrue(dir.toString().contains("baseDir"));
    }

    @Test
    void testCreateOutputStreamAndRead() throws IOException {
        String relPath = "stream.txt";
        try (var out = dir.createOutputStream(relPath)) {
            out.write("stream-data".getBytes(StandardCharsets.UTF_8));
        }
        String result = dir.readText(relPath);
        assertEquals("stream-data", result);
    }

    @Test
    void testGetLocal() {
        Directory localDir = Directory.getLocal(Path.of("unit-test"));
        assertNotNull(localDir);
        assertTrue(localDir.base().toString().contains("unit-test"));
    }
}
