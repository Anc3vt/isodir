# Isolated Directory Library


[![License](https://img.shields.io/github/license/Anc3vt/isolated-directory)](https://www.apache.org/licenses/LICENSE-2.0)


> Simple, safe, cross-platform isolated file system utility for Java.

## ✨ Features

- Clean API for reading/writing files and directories
- Handles relative paths safely (prevents `../` traversal)
- Works across platforms (Windows, macOS, Linux)
- Charset-aware text IO
- JUnit 5 tested, minimal dependencies

## 🚀 Getting Started

### Maven

```xml
<dependency>
    <groupId>com.ancevt.util</groupId>
    <artifactId>isolated-directory</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.ancevt.util:isolated-directory:1.0.1")
}
```

## 💡 Example

```java
IsolatedDirectory dir = new IsolatedDirectory("/tmp/mydata");
dir.writeText("hello.txt", "Hello World!");
System.out.println(dir.readText("hello.txt"));
```

## 📦 API Highlights

```java
writeText(String path, String text)
readText(String path)
appendText(String path, String text)
writeBytes(String path, byte[] data)
readBytes(String path)
delete(String path)
createDir(String path)
deleteDir(String path)
createOutputStream(String path, boolean overwrite)
```

## 🧪 Test Coverage

- Full unit tests with `JUnit 5`
- `@TempDir` for test isolation
- Parameterized charset tests

## 📄 License

Apache 2.0. See LICENSE for details.

## 🤝 Contributing

PRs welcome. Issues tolerated. Opinions ignored. Just kidding (mostly).

## 🐛 Reporting Bugs

Feel free to open [Issues](https://github.com/Anc3vt/isolated-directory/issues) or submit pull requests.

