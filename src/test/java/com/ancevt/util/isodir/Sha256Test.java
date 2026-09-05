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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Sha256Test {

    private static final String EMPTY_SHA256 =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String ABC_SHA256 =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String LARGE_SHA256 =
            "7692ef7b60c99df002333ba44eae54362c05921b7510ef2f9ef405500723473c";

    @TempDir
    Path tempDir;

    @Test
    void hashesKnownVectorsInFilesystemAndMemory() {
        for (IsolatedDirectory directory : directories()) {
            directory.writeBytes("empty.bin", new byte[0]);
            directory.writeBytes("abc.bin", "abc".getBytes(StandardCharsets.UTF_8));

            assertArrayEquals(fromHex(EMPTY_SHA256), directory.sha256("empty.bin"));
            assertArrayEquals(fromHex(ABC_SHA256), directory.sha256("abc.bin"));
            assertEquals(0, directory.getSize("empty.bin"));
            assertEquals(3, directory.getSize("abc.bin"));
            assertEquals(32, directory.sha256("abc.bin").length);
            assertNotSame(directory.sha256("abc.bin"), directory.sha256("abc.bin"));
        }
    }

    @Test
    void hashesNestedLargeFileIdenticallyInFilesystemAndMemory() {
        byte[] bytes = patternedBytes(32785);
        byte[] expected = fromHex(LARGE_SHA256);
        byte[][] digests = new byte[2][];
        IsolatedDirectory[] directories = directories();

        for (int i = 0; i < directories.length; i++) {
            IsolatedDirectory directory = directories[i];
            directory.createDir("maps/sectors");
            directory.writeBytes("maps/sectors/sector.bdf", bytes);
            digests[i] = directory.sha256("maps/sectors/sector.bdf");
            assertArrayEquals(expected, digests[i]);
        }

        assertArrayEquals(digests[0], digests[1]);
    }

    @Test
    void rejectsMissingFilesAndDirectoriesInFilesystemAndMemory() {
        for (IsolatedDirectory directory : directories()) {
            assertThrows(IsolatedDirectoryException.class,
                    () -> directory.sha256("missing.bin"));
            assertThrows(IsolatedDirectoryException.class,
                    () -> directory.getSize("missing.bin"));

            directory.createDir("directory-only");
            assertThrows(IsolatedDirectoryException.class,
                    () -> directory.sha256("directory-only"));
            assertThrows(IsolatedDirectoryException.class,
                    () -> directory.getSize("directory-only"));
        }
    }

    private IsolatedDirectory[] directories() {
        return new IsolatedDirectory[]{
                new IsolatedDirectory(tempDir.resolve("filesystem")),
                new InMemoryIsolatedDirectory()
        };
    }

    private static byte[] patternedBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 31 + 7);
        }
        return bytes;
    }

    private static byte[] fromHex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int offset = i * 2;
            bytes[i] = (byte) Integer.parseInt(value.substring(offset, offset + 2), 16);
        }
        return bytes;
    }
}
