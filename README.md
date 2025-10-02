# IsolatedDirectory

[![Maven Central](https://img.shields.io/maven-central/v/com.ancevt.util/isodir.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.ancevt.util/isodir)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Java](https://img.shields.io/badge/Java-8%2B-brightgreen)
![Build](https://img.shields.io/badge/build-passing-success)

A lightweight, dependency-free Java library for safe and sandboxed file I/O operations.
Originally built as a local storage layer for a game project, it provides a clean abstraction for creating, reading, writing, and deleting files and directories — all within a restricted base path.

> Think of it as a little filesystem world where your app can live, save its progress, and never leak into the real one.

## 📚 Table of Contents

* [✨ Features](#-features)
* [📦 Installation](#-installation)
* [🚀 Use Cases](#-use-cases)

  * [🎮 Game Save System](#-1-game-save-system)
  * [⚙️ Config File Handling](#-2-config-file-handling)
  * [📁 Dynamic Asset Caching](#-3-dynamic-asset-caching)
* [❗ Safety by Design](#-safety-by-design)
* [🧩 In-Memory Isolated Directory](#-in-memory-isolated-directory)

  * [✨ Features](#-features-1)
  * [🚀 Use Cases](#-use-cases-1)

    * [🧪 Unit Testing](#-1-unit-testing)
    * [⚡ Ephemeral Caches](#-2-ephemeral-caches)
    * [🛠️ Prototyping File Logic](#-3-prototyping-file-logic)
  * [💾 Serialization Support](#-serialization-support)
  * [🧠 When to Use](#-when-to-use)
* [🧠 Motivation](#-motivation)
* [📃 License](#-license)
* [💬 Feedback & Contributions](#-feedback--contributions)


---

## ✨ Features

* **Sandboxed Base Directory**

    * All file operations are strictly scoped to a predefined base directory.
    * Path traversal (`../../`) is explicitly blocked.

* **Easy File I/O**

    * `writeText`, `readBytes`, `appendText`, `createOutputStream`, etc.
    * Overloads for charset-aware operations (UTF-8 by default).

* **Automatic Directory Creation**

    * Ensures intermediate directories exist before writing.

* **Safe Deletion**

    * Delete single files or recursively delete entire subdirectories.

* **Platform-Aware Local Storage Path**

    * `IsolatedDirectory.getLocal("my-game")` resolves to OS-specific app data folder.

* **No external dependencies**

    * Fully compatible with Java 8+

---

## 📦 Installation

Available via Maven Central:

```xml
<dependency>
    <groupId>com.ancevt.util</groupId>
    <artifactId>isodir</artifactId>
    <version>1.1.0</version>
</dependency>
```

---

## 🚀 Use Cases

### 🎮 1. Game Save System

```java
IsolatedDirectory saves = IsolatedDirectory.getLocal("my-game/saves");
saves.writeText("slot1.json", saveDataJson);
```

### ⚙️ 2. Config File Handling

```java
IsolatedDirectory configDir = new IsolatedDirectory("config");
configDir.writeText("settings.ini", "volume=80\ndebug=true");
```

### 📁 3. Dynamic Asset Caching

```java
IsolatedDirectory cache = new IsolatedDirectory("./.cache");
if (!cache.exists("map_42.png")) {
    byte[] bytes = downloadFromServer();
    cache.writeBytes("map_42.png", bytes);
}
```
---

## ❗ Safety by Design

* All paths are resolved and normalized against the base.
* Any attempt to escape the base directory throws an `IsolatedDirectoryException`.
* OutputStreams default to overwrite mode unless specified.

---

## 🧩 In-Memory Isolated Directory

In addition to the real filesystem-backed `IsolatedDirectory`, this library also provides an **in-memory implementation**: `InMemoryIsolatedDirectory`.

This variant simulates a sandboxed directory structure entirely in memory. It’s perfect for situations where persistence is not required, but the same API is desired.

---

### ✨ Features

* **No Disk Writes**

  * Everything lives in memory; nothing touches the real filesystem.
* **Identical API**

  * Implements the same methods as `IsolatedDirectory` (`writeBytes`, `readText`, `appendBytes`, etc.).
* **Ephemeral**

  * Data disappears once the instance is discarded.
* **Debug-Friendly**

  * Tree dump via `toString()` for quick inspection.

---

### 🚀 Use Cases

#### 🧪 1. Unit Testing

```java
InMemoryIsolatedDirectory dir = new InMemoryIsolatedDirectory();
dir.writeText("test/file.txt", "hello");
assertEquals("hello", dir.readText("test/file.txt"));
```

No temp files. No cleanup headaches. Perfect for fast, isolated tests.

#### ⚡ 2. Ephemeral Caches

```java
InMemoryIsolatedDirectory cache = new InMemoryIsolatedDirectory();
cache.writeBytes("image.png", downloadedBytes);
// Cache disappears when the app closes
```

Keep hot data close without ever writing to disk.

#### 🛠️ 3. Prototyping File Logic

```java
InMemoryIsolatedDirectory proto = new InMemoryIsolatedDirectory();
proto.writeText("config/settings.json", "{ \"debug\": true }");
System.out.println(proto);
```

Quickly simulate directory structures while designing file-based APIs.

---

### 💾 Serialization Support

`InMemoryIsolatedDirectory` can also **save/load snapshots**:

* Save to a real directory on disk.
* Save to a single binary file.
* Load snapshots back into memory.

This makes it possible to persist ephemeral states when needed.

---

### 🧠 When to Use

* You want the *same* API as `IsolatedDirectory` but without touching disk.
* You need quick, clean storage for tests, experiments, or caches.
* You want to simulate complex directory trees without creating real files.

> Think of it as a whiteboard for your filesystem: quick sketches, instant erasure.


## 🧠 Motivation

Sometimes, all you need is a tiny, private world where your app can create and destroy files without worrying about where they're really going.

This library was built out of necessity — to give a game a place to live. Now it's here for you too.

---

## 📃 License

Apache License 2.0

---

## 💬 Feedback & Contributions

PRs and issues welcome. This lib is small, sharp, and opinionated. If you’re into that — let’s talk.
