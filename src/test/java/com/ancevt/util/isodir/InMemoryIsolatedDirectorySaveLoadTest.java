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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryIsolatedDirectory Save/Load File Tests")
class InMemoryIsolatedDirectorySaveLoadTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should save and load simple file")
    void saveAndLoadSimpleFile() throws IOException {
        InMemoryIsolatedDirectory dir = new InMemoryIsolatedDirectory();
        dir.writeText("file.txt", "hello");

        Path snapshot = tempDir.resolve("snapshot.bin");
        dir.saveToFile(snapshot);

        assertTrue(Files.exists(snapshot));

        InMemoryIsolatedDirectory loaded = new InMemoryIsolatedDirectory();
        loaded.loadFromFile(snapshot);

        assertEquals("hello", loaded.readText("file.txt"));
    }

    @Test
    @DisplayName("should save and load nested directories and multiple files")
    void saveAndLoadNested() throws IOException {
        InMemoryIsolatedDirectory dir = new InMemoryIsolatedDirectory();
        dir.writeText("foo/bar.txt", "bar-data");
        dir.writeText("foo/baz/qux.txt", "deep-data");
        dir.writeBytes("bin/raw.dat", new byte[]{1, 2, 3, 4});

        Path snapshot = tempDir.resolve("nested.bin");
        dir.saveToFile(snapshot);

        InMemoryIsolatedDirectory loaded = new InMemoryIsolatedDirectory();
        loaded.loadFromFile(snapshot);

        assertEquals("bar-data", loaded.readText("foo/bar.txt"));
        assertEquals("deep-data", loaded.readText("foo/baz/qux.txt"));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, loaded.readBytes("bin/raw.dat"));
    }

    @Test
    @DisplayName("should handle empty directory save/load")
    void saveAndLoadEmptyDirectory() throws IOException {
        InMemoryIsolatedDirectory dir = new InMemoryIsolatedDirectory();

        Path snapshot = tempDir.resolve("empty.bin");
        dir.saveToFile(snapshot);

        InMemoryIsolatedDirectory loaded = new InMemoryIsolatedDirectory();
        loaded.loadFromFile(snapshot);

        assertFalse(loaded.exists("anything.txt"));
        assertEquals("InMemoryIsolatedDirectory{}", loaded.toString().trim());
    }

    @Test
    @DisplayName("save/load should preserve overwrites")
    void saveAndLoadOverwrite() throws IOException {
        InMemoryIsolatedDirectory dir = new InMemoryIsolatedDirectory();
        dir.writeText("file.txt", "first");
        dir.writeText("file.txt", "second");

        Path snapshot = tempDir.resolve("overwrite.bin");
        dir.saveToFile(snapshot);

        InMemoryIsolatedDirectory loaded = new InMemoryIsolatedDirectory();
        loaded.loadFromFile(snapshot);

        assertEquals("second", loaded.readText("file.txt"));
    }
}
