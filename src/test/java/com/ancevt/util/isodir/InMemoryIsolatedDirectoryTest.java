/*
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryIsolatedDirectory Tests")
class InMemoryIsolatedDirectoryTest {

    InMemoryIsolatedDirectory dir;

    @BeforeEach
    void setUp() {
        dir = new InMemoryIsolatedDirectory();
    }

    @Nested
    @DisplayName("Read/Write Operations")
    class ReadWriteTests {

        @Test
        @DisplayName("should write and read text")
        void writeAndReadText() {
            dir.writeText("foo.txt", "bar");
            assertEquals("bar", dir.readText("foo.txt"));
        }

        @Test
        @DisplayName("should write and read bytes")
        void writeAndReadBytes() {
            byte[] data = {1, 2, 3};
            dir.writeBytes("bin.dat", data);
            assertArrayEquals(data, dir.readBytes("bin.dat"));
        }

        @Test
        @DisplayName("should append bytes correctly")
        void appendBytes() {
            dir.writeBytes("append.dat", "abc".getBytes(StandardCharsets.UTF_8));
            dir.appendBytes("append.dat", "def".getBytes(StandardCharsets.UTF_8));
            assertEquals("abcdef", dir.readText("append.dat"));
        }

        @Test
        @DisplayName("should overwrite or append via OutputStream")
        void createOutputStreamTest() throws IOException {
            try (OutputStream out = dir.createOutputStream("stream.txt", true)) {
                out.write("one".getBytes(StandardCharsets.UTF_8));
            }
            try (OutputStream out = dir.createOutputStream("stream.txt", false)) {
                out.write(" two".getBytes(StandardCharsets.UTF_8));
            }
            assertEquals("one two", dir.readText("stream.txt"));
        }
    }

    @Nested
    @DisplayName("Directory Handling")
    class DirectoryOps {

        @Test
        @DisplayName("should create subdirectories")
        void createDirs() {
            dir.createDir("nested/dir");
            assertTrue(dir.exists("nested"));
            assertTrue(dir.exists("nested/dir"));
        }

        @Test
        @DisplayName("should delete file")
        void deleteFile() {
            dir.writeText("deleteme.txt", "bye");
            assertTrue(dir.exists("deleteme.txt"));
            dir.delete("deleteme.txt");
            assertFalse(dir.exists("deleteme.txt"));
        }

        @Test
        @DisplayName("should delete directory tree")
        void deleteDirTree() {
            dir.writeText("folder/file.txt", "x");
            assertTrue(dir.exists("folder/file.txt"));
            dir.deleteDir("folder");
            assertFalse(dir.exists("folder/file.txt"));
            assertFalse(dir.exists("folder"));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorCases {

        @Test
        @DisplayName("should throw on reading nonexistent file")
        void readNonexistent() {
            assertThrows(IsolatedDirectoryException.class,
                    () -> dir.readText("ghost.txt"));
        }

        @Test
        @DisplayName("should throw when path is a directory instead of file")
        void directoryInsteadOfFile() {
            dir.createDir("somedir");
            assertThrows(IsolatedDirectoryException.class,
                    () -> dir.readBytes("somedir"));
        }
    }

    @Nested
    @DisplayName("Utility")
    class UtilityTests {

        @Test
        @DisplayName("should produce a readable tree dump in toString()")
        void toStringTree() {
            dir.writeText("foo/bar.txt", "hi");
            String out = dir.toString();
            assertTrue(out.contains("bar.txt"));
            assertTrue(out.contains("[FILE]"));
            assertTrue(out.contains("[DIR]"));
        }
    }
}
