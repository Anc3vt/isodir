/**
 * Copyright (C) 2025 Ancevt.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ancevt.util.isodir;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IsolatedDirectory Tests")
class IsolatedDirectoryTest {

    @TempDir
    Path tempDir;

    IsolatedDirectory dir;

    @BeforeEach
    void setUp() {
        dir = new IsolatedDirectory(tempDir);
    }

    @Nested
    @DisplayName("Read/Write Operations")
    class ReadWriteTests {

        @Test
        @DisplayName("should write and read text file")
        void writeAndReadText() {
            String relPath = "test.txt";
            String content = "Hello, Directory!";
            dir.writeText(relPath, content);

            String read = dir.readText(relPath);
            assertEquals(content, read);
        }

        @Test
        @DisplayName("should write and read bytes")
        void writeAndReadBytes() {
            String relPath = "binfile.bin";
            byte[] bytes = {1, 2, 3, 4, 5};
            dir.writeBytes(relPath, bytes);

            byte[] read = dir.readBytes(relPath);
            assertArrayEquals(bytes, read);
        }

        @Test
        @DisplayName("should append text and bytes correctly")
        void appendTextAndBytes() {
            String relPath = "append.txt";

            dir.writeText(relPath, "abc");
            dir.appendText(relPath, "def");
            assertEquals("abcdef", dir.readText(relPath));

            dir.writeBytes(relPath, "123".getBytes(StandardCharsets.UTF_8));
            dir.appendBytes(relPath, "456".getBytes(StandardCharsets.UTF_8));
            assertEquals("123456", dir.readText(relPath));
        }

        @Test
        @DisplayName("should overwrite or append via OutputStream")
        void createOutputStreamTest() throws IOException {
            String relPath = "stream.txt";

            try (OutputStream out = dir.createOutputStream(relPath, true)) {
                out.write("overwrite".getBytes(StandardCharsets.UTF_8));
            }

            try (OutputStream out = dir.createOutputStream(relPath, false)) {
                out.write(" and append".getBytes(StandardCharsets.UTF_8));
            }

            String result = dir.readText(relPath);
            assertEquals("overwrite and append", result);
        }

        @ParameterizedTest(name = "should handle charset = {0}")
        @ValueSource(strings = {"UTF-8", "US-ASCII"})
        void writeWithCharset(String charsetName) {
            String relPath = "enc.txt";
            String text = "hello";
            dir.writeText(relPath, text, Charset.forName(charsetName));
            String read = dir.readText(relPath, Charset.forName(charsetName));
            assertEquals(text, read);
        }
    }

    @Nested
    @DisplayName("Directory and Existence Handling")
    class DirectoryOps {

        @Test
        @DisplayName("should create subdirectories")
        void createDirs() {
            String subDir = "sub/dir";
            assertFalse(dir.exists(subDir));
            dir.createDir(subDir);
            assertTrue(dir.exists(subDir));
        }

        @Test
        @DisplayName("should delete single file")
        void deleteFile() {
            String relPath = "deleteMe.txt";
            dir.writeText(relPath, "to delete");
            assertTrue(dir.exists(relPath));
            dir.delete(relPath);
            assertFalse(dir.exists(relPath));
        }

        @Test
        @DisplayName("should delete directory tree")
        void deleteDirTree() {
            String relDir = "folder/to/delete";
            dir.createDir(relDir);
            dir.writeText(relDir + "/file.txt", "data");
            assertTrue(dir.exists(relDir + "/file.txt"));

            dir.deleteDir("folder");
            assertFalse(dir.exists("folder"));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorCases {

        @Test
        @DisplayName("should throw on reading nonexistent file")
        void readNonexistent() {
            String relPath = "nope.txt";
            assertThrows(IsolatedDirectoryException.class, () -> dir.readText(relPath));
            assertThrows(IsolatedDirectoryException.class, () -> dir.readBytes(relPath));
            assertThrows(IsolatedDirectoryException.class, () -> dir.read(relPath));
        }

        @Test
        @DisplayName("should throw on path traversal")
        void preventPathTraversal() {
            assertThrows(IsolatedDirectoryException.class, () -> dir.writeText("../../evil.txt", "hack"));
        }
    }

    @Nested
    @DisplayName("Utility")
    class UtilityTests {

        @Test
        @DisplayName("should include baseDir in toString()")
        void toStringIncludesPath() {
            String out = dir.toString();
            assertNotNull(out);
            assertTrue(out.contains("baseDir"));
        }

        @Test
        @DisplayName("should resolve local app path")
        void getLocalAppPath() {
            IsolatedDirectory local = IsolatedDirectory.getLocal("my-test");
            assertTrue(local.base().toString().contains("my-test"));
        }
    }
}
