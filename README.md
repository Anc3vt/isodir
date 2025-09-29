# IsolatedDirectory

A lightweight, dependency-free Java library for safe and sandboxed file I/O operations.
Originally built as a local storage layer for a game project, it provides a clean abstraction for creating, reading, writing, and deleting files and directories — all within a restricted base path.

> Think of it as a little filesystem world where your app can live, save its progress, and never leak into the real one.

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
    <version>1.0.1</version>
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

## 🧠 Motivation

Sometimes, all you need is a tiny, private world where your app can create and destroy files without worrying about where they're really going.

This library was built out of necessity — to give a game a place to live. Now it's here for you too.

---

## 📃 License

Apache License 2.0

---

## 💬 Feedback & Contributions

PRs and issues welcome. This lib is small, sharp, and opinionated. If you’re into that — let’s talk.
