# IsolatedDirectory

[![Maven Central](https://img.shields.io/maven-central/v/com.ancevt.util/isodir.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.ancevt.util/isodir)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
![Java](https://img.shields.io/badge/Java-8%2B-brightgreen)

IsolatedDirectory (`isodir`) is a small, dependency-free Java library for file I/O scoped to a single base directory. It provides the same core storage operations for a real filesystem and an in-memory implementation, making application storage code easy to reuse in production and tests.

## Features

- Resolves application paths relative to a fixed base directory.
- Rejects `..` path traversal and symbolic links encountered while resolving a path.
- Reads, writes, and appends bytes or UTF-8 text.
- Exposes streaming input and output for large files.
- Creates directories and supports single-path or recursive deletion.
- Reports file size without loading the file into memory.
- Computes SHA-256 incrementally and returns the 32-byte digest.
- Locates an OS-specific application-data directory with `getLocal(...)`.
- Includes an in-memory implementation with snapshot import/export support.
- Requires Java 8 or newer and has no runtime dependencies.

## Installation

The latest release available from Maven Central is `1.1.2`:

```xml
<dependency>
    <groupId>com.ancevt.util</groupId>
    <artifactId>isodir</artifactId>
    <version>1.1.2</version>
</dependency>
```
## Quick start

All operation paths are relative to the configured base directory:

```java
import com.ancevt.util.isodir.IsolatedDirectory;

IsolatedDirectory storage = new IsolatedDirectory("data");
storage.createDir("config");
storage.createDir("logs");

storage.writeText("config/settings.properties", "volume=80\n");
storage.appendText("logs/latest.log", "Application started\n");

String settings = storage.readText("config/settings.properties");
byte[] contents = storage.readBytes("config/settings.properties");
long size = storage.getSize("config/settings.properties");
byte[] digest = storage.sha256("config/settings.properties");
```

Parent directories are not created automatically by file-writing methods. Create them explicitly before writing nested files.

## Main API

| Operation | Methods |
| --- | --- |
| Base and path handling | `base()`, `resolve(path)`, `getLocal(path)` |
| Existence and directories | `exists(path)`, `createDir(path)` |
| Byte I/O | `readBytes(path)`, `writeBytes(path, data)`, `appendBytes(path, data)` |
| Text I/O | `readText(path)`, `writeText(path, text)`, `appendText(path, text)` |
| Streaming I/O | `read(path)`, `createOutputStream(path[, overwrite])` |
| File metadata | `getSize(path)`, `sha256(path)` |
| Deletion | `delete(path)`, `deleteDir(path)` |

Text methods use UTF-8 by default and also provide overloads accepting a `Charset`. `createOutputStream(path)` overwrites by default; pass `false` as the second argument to append.

The caller owns streams returned by `read(...)` and `createOutputStream(...)` and must close them:

```java
try (InputStream input = storage.read("assets/map.bdf")) {
    // Consume the file without allocating one array for the entire payload.
}
```

## File size and SHA-256

`getSize(path)` accepts regular files and returns their raw size in bytes. `sha256(path)` reads the file through a bounded buffer, so it is suitable for checking large cached assets.

```java
byte[] expectedHash = downloadMetadataHash();

if (storage.exists("cache/map.bdf")
        && storage.getSize("cache/map.bdf") > 0
        && Arrays.equals(storage.sha256("cache/map.bdf"), expectedHash)) {
    // The cached file matches the expected content.
}
```

Both methods throw `IsolatedDirectoryException` when the target cannot be accessed. `getSize(...)` also rejects directories and other non-regular paths.

## OS-specific local storage

`getLocal(...)` creates an isolated view below the operating system's application-data location:

```java
IsolatedDirectory saves = IsolatedDirectory.getLocal("my-game/saves");
saves.writeText("slot-1.json", saveDataJson);
```

The resulting base is platform-dependent. Call `base()` when the concrete path is needed for diagnostics.

## In-memory storage

`InMemoryIsolatedDirectory` extends `IsolatedDirectory` and supports the same common read/write, streaming, size, hashing, and deletion operations without touching the filesystem:

```java
import com.ancevt.util.isodir.InMemoryIsolatedDirectory;

InMemoryIsolatedDirectory storage = new InMemoryIsolatedDirectory();
storage.createDir("profiles");
storage.writeText("profiles/player.txt", "Player One");

assert storage.getSize("profiles/player.txt") == 10;
assert storage.readText("profiles/player.txt").equals("Player One");
```

This is useful for unit tests, temporary caches, and prototypes. Its `toString()` method prints the current directory tree for debugging.

### In-memory snapshots

An in-memory tree can be copied to or loaded from a real directory:

```java
storage.save(Paths.get("backup-directory"));
storage.load(Paths.get("backup-directory"));
```

It can also be serialized to one binary file and restored later:

```java
storage.saveToFile(Paths.get("storage.snapshot"));
storage.loadFromFile(Paths.get("storage.snapshot"));
```

`load(...)` and `loadFromFile(...)` replace the current in-memory contents. Snapshot methods expose `IOException`, unlike normal storage operations, which wrap I/O failures in `IsolatedDirectoryException`.

## Safety model

`IsolatedDirectory` normalizes its absolute base path and resolves every operation below it. A path containing a `..` segment is rejected, as is a path that encounters an existing symbolic link. Invalid paths and ordinary filesystem failures are reported as unchecked `IsolatedDirectoryException` instances.

This is a narrow application-storage boundary, not a complete security sandbox for hostile code. In particular, callers can obtain the underlying base `Path`, and filesystem state can change concurrently between validation and an operation.

## Building and testing

```shell
mvn clean test
```

To install the current artifact locally for another Maven project:

```shell
mvn clean install
```

## License

Licensed under the [Apache License 2.0](LICENSE).

Contributions and issue reports are welcome.
