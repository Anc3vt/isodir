package com.ancevt.util.isodir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An in-memory implementation of {@link IsolatedDirectory}.
 * <p>
 * This class simulates a sandboxed directory structure entirely in memory.
 * Files and directories are represented as a simple tree of nodes, so
 * no changes are ever written to the physical filesystem.
 * </p>
 *
 * <h2>Use cases:</h2>
 * <ul>
 *   <li>Unit testing code that relies on {@link IsolatedDirectory} without
 *   touching the real disk.</li>
 *   <li>Ephemeral storage when persistence is not required.</li>
 *   <li>Debugging and prototyping file-based logic.</li>
 * </ul>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Supports nested directories and files with byte[] or text contents.</li>
 *   <li>Implements the same API as {@link IsolatedDirectory}:
 *       {@code readBytes()}, {@code writeBytes()}, {@code appendBytes()},
 *       {@code createOutputStream()}, etc.</li>
 *   <li>Tree dump via {@link #toString()} for debugging.</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * InMemoryIsolatedDirectory dir = new InMemoryIsolatedDirectory();
 * dir.writeText("foo/bar.txt", "Hello!");
 * System.out.println(dir.readText("foo/bar.txt")); // Hello!
 * }</pre>
 *
 * <p>
 * Note: contents live only as long as this object instance. Once it is
 * discarded, all files are lost.
 * </p>
 */
public class InMemoryIsolatedDirectory extends IsolatedDirectory {

    private final DirectoryNode root = new DirectoryNode("/");

    public InMemoryIsolatedDirectory() {
        super("/in-memory");
    }

    private String normalizePath(String relativePath) {
        return relativePath.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+", "/").trim();
    }

    /**
     * Resolves a node by path. Creates missing directories if requested.
     */
    private Node resolveNode(String relativePath, boolean createDirs) {
        if (relativePath == null || relativePath.isEmpty()) return root;
        String[] parts = normalizePath(relativePath).split("/");
        DirectoryNode current = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean last = (i == parts.length - 1);

            Node next = current.children.get(part);

            if (next == null) {
                if (createDirs) {
                    DirectoryNode dir = new DirectoryNode(part);
                    current.children.put(part, dir);
                    next = dir;
                } else {
                    return null;
                }
            }

            if (next instanceof DirectoryNode) {
                if (last) return next;
                current = (DirectoryNode) next;
            } else {
                if (!last) throw new IsolatedDirectoryException("Expected directory at: " + part);
                return next;
            }
        }
        return current;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns true if a file or directory exists in memory at the given path.
     * </p>
     */
    @Override
    public boolean exists(String relativePath) {
        return resolveNode(relativePath, false) != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Reads the content of a file into a new byte array.
     * </p>
     *
     * @throws IsolatedDirectoryException if the path does not exist or is not a file
     */
    @Override
    public byte[] readBytes(String relativePath) {
        Node node = resolveNode(relativePath, false);
        if (!(node instanceof FileNode)) {
            throw new IsolatedDirectoryException("File does not exist: " + relativePath);
        }
        FileNode file = (FileNode) node;
        return Arrays.copyOf(file.data, file.data.length);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates or replaces a file with the given byte contents.
     * Intermediate directories are created if necessary.
     * </p>
     */
    @Override
    public void writeBytes(String relativePath, byte[] bytes) {
        String[] parts = normalizePath(relativePath).split("/");
        DirectoryNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Node child = parent.children.get(parts[i]);
            if (child == null) {
                DirectoryNode dir = new DirectoryNode(parts[i]);
                parent.children.put(parts[i], dir);
                parent = dir;
            } else if (child instanceof DirectoryNode) {
                parent = (DirectoryNode) child;
            } else {
                throw new IsolatedDirectoryException("Cannot create directory inside file: " + parts[i]);
            }
        }
        String fileName = parts[parts.length - 1];
        parent.children.put(fileName, new FileNode(fileName, Arrays.copyOf(bytes, bytes.length)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Appends bytes to an existing file. If the file does not exist, it is created.
     * </p>
     */
    @Override
    public void appendBytes(String relativePath, byte[] bytes) {
        Node node = resolveNode(relativePath, false);
        if (node == null) {
            writeBytes(relativePath, bytes);
            return;
        }
        if (!(node instanceof FileNode)) {
            throw new IsolatedDirectoryException("Path is not a file: " + relativePath);
        }
        FileNode file = (FileNode) node;
        byte[] combined = new byte[file.data.length + bytes.length];
        System.arraycopy(file.data, 0, combined, 0, file.data.length);
        System.arraycopy(bytes, 0, combined, file.data.length, bytes.length);
        file.data = combined;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a fresh {@link InputStream} for reading the contents of a file.
     * </p>
     */
    @Override
    public InputStream read(String relativePath) {
        return new ByteArrayInputStream(readBytes(relativePath));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns an {@link OutputStream} that writes into a file in memory.
     * When the stream is closed, the data is either written or appended.
     * </p>
     *
     * @param relativePath file path
     * @param overwrite    if true, replaces file contents; if false, appends
     */
    @Override
    public OutputStream createOutputStream(final String relativePath, final boolean overwrite) {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                byte[] data = toByteArray();
                if (overwrite) {
                    InMemoryIsolatedDirectory.this.writeBytes(relativePath, data);
                } else {
                    InMemoryIsolatedDirectory.this.appendBytes(relativePath, data);
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Deletes a file or directory node by path. Does nothing if it does not exist.
     * </p>
     */
    @Override
    public void delete(String relativePath) {
        String[] parts = normalizePath(relativePath).split("/");
        DirectoryNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Node child = parent.children.get(parts[i]);
            if (!(child instanceof DirectoryNode)) {
                return; // path not found → no-op, как и в IsolatedDirectory
            }
            parent = (DirectoryNode) child;
        }

        String name = parts[parts.length - 1];
        Node target = parent.children.get(name);

        if (target == null) {
            return; // nothing to delete
        }

        if (target instanceof DirectoryNode) {
            DirectoryNode dir = (DirectoryNode) target;
            if (!dir.children.isEmpty()) {
                throw new IsolatedDirectoryException(
                        "Directory is not empty: " + relativePath
                );
            }
        }

        parent.children.remove(name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * In this in-memory implementation, directory deletion is identical to
     * {@link #delete(String)} — the node (file or directory) at the given path
     * is removed from its parent.
     * </p>
     *
     * @param relativePath relative path of the directory to delete
     */
    @Override
    public void deleteDir(String relativePath) {
        Node node = resolveNode(relativePath, false);
        if (node == null) return;

        if (!(node instanceof DirectoryNode)) {
            throw new IsolatedDirectoryException("Not a directory: " + relativePath);
        }

        deleteRecursive((DirectoryNode) node);

        String[] parts = normalizePath(relativePath).split("/");
        DirectoryNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Node child = parent.children.get(parts[i]);
            if (!(child instanceof DirectoryNode)) return;
            parent = (DirectoryNode) child;
        }
        parent.children.remove(parts[parts.length - 1]);
    }

    private void deleteRecursive(DirectoryNode dir) {
        for (Node child : dir.children.values().toArray(new Node[0])) {
            if (child instanceof DirectoryNode) {
                deleteRecursive((DirectoryNode) child);
            }
        }
        dir.children.clear();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates a directory (and intermediate directories) in memory.
     * </p>
     *
     * @return pseudo-{@link Path} pointing to the created directory
     */
    @Override
    public Path createDir(String relativePath) {
        resolveNode(relativePath, true);
        return Paths.get("/in-memory", normalizePath(relativePath));
    }

    @Override
    public String toString() {
        return "InMemoryIsolatedDirectory{" + dumpTree(root, 0) + "}";
    }

    private String dumpTree(DirectoryNode dir, int indent) {
        StringBuilder sb = new StringBuilder();
        String pad = repeat(" ", indent);
        for (Node node : dir.children.values()) {
            if (node instanceof DirectoryNode) {
                sb.append("\n").append(pad).append("[DIR] ").append(node.name);
                sb.append(dumpTree((DirectoryNode) node, indent + 2));
            } else if (node instanceof FileNode) {
                sb.append("\n").append(pad).append("[FILE] ").append(node.name)
                        .append(" (").append(((FileNode) node).data.length).append(" bytes)");
            }
        }
        return sb.toString();
    }

    private static String repeat(String s, int count) {
        if (count <= 0) return "";
        char[] buf = new char[s.length() * count];
        for (int i = 0; i < count; i++) {
            s.getChars(0, s.length(), buf, i * s.length());
        }
        return new String(buf);
    }

    /**
     * Saves the current in-memory directory tree to a real filesystem directory.
     *
     * @param targetDir the directory on disk where everything should be saved
     * @throws IOException if any I/O error occurs
     */
    public void save(Path targetDir) throws IOException {
        if (!targetDir.toFile().exists()) {
            targetDir.toFile().mkdirs();
        }
        saveRecursive(root, targetDir);
    }

    private void saveRecursive(DirectoryNode dir, Path target) throws IOException {
        for (Node node : dir.children.values()) {
            if (node instanceof DirectoryNode) {
                Path subdir = target.resolve(node.name);
                if (!subdir.toFile().exists()) {
                    subdir.toFile().mkdirs();
                }
                saveRecursive((DirectoryNode) node, subdir);
            } else if (node instanceof FileNode) {
                Path filePath = target.resolve(node.name);
                try (OutputStream out = Files.newOutputStream(filePath.toFile().toPath())) {
                    out.write(((FileNode) node).data);
                }
            }
        }
    }

    /**
     * Loads the contents of a real filesystem directory into this in-memory directory.
     * Existing in-memory contents will be cleared.
     *
     * @param sourceDir the real directory to load from
     * @throws IOException if any I/O error occurs
     */
    public void load(Path sourceDir) throws IOException {
        if (!sourceDir.toFile().exists() || !sourceDir.toFile().isDirectory()) {
            throw new IOException("Source directory does not exist: " + sourceDir);
        }
        root.children.clear();
        loadRecursive(root, sourceDir);
    }

    private void loadRecursive(DirectoryNode parent, Path source) throws IOException {
        File[] files = source.toFile().listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                DirectoryNode dirNode = new DirectoryNode(f.getName());
                parent.children.put(f.getName(), dirNode);
                loadRecursive(dirNode, f.toPath());
            } else if (f.isFile()) {
                byte[] data = readFileBytes(f);
                FileNode fileNode = new FileNode(f.getName(), data);
                parent.children.put(f.getName(), fileNode);
            }
        }
    }

    private static byte[] readFileBytes(File file) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            return out.toByteArray();
        }
    }

    /**
     * Saves the entire in-memory tree into a single binary file.
     *
     * @param file target file
     */
    public void saveToFile(Path file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            saveNode(out, root);
        }
    }

    private void saveNode(DataOutputStream out, DirectoryNode dir) throws IOException {
        // write directory start
        out.writeByte(0); // type=0 (directory)
        out.writeUTF(dir.name);
        out.writeInt(dir.children.size());

        for (Node child : dir.children.values()) {
            if (child instanceof DirectoryNode) {
                saveNode(out, (DirectoryNode) child);
            } else if (child instanceof FileNode) {
                FileNode f = (FileNode) child;
                out.writeByte(1); // type=1 (file)
                out.writeUTF(f.name);
                out.writeInt(f.data.length);
                out.write(f.data);
            }
        }
    }

    /**
     * Loads a tree previously saved with {@link #saveToFile(Path)}.
     * Existing contents are cleared.
     *
     * @param file source file
     */
    public void loadFromFile(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            root.children.clear();
            loadNode(in, root);
        }
    }

    private void loadNode(DataInputStream in, DirectoryNode parent) throws IOException {
        byte type = in.readByte();
        if (type != 0) {
            throw new IOException("Root must be a directory");
        }
        String name = in.readUTF(); // for root it's "/"
        int childCount = in.readInt();

        for (int i = 0; i < childCount; i++) {
            byte childType = in.readByte();
            if (childType == 0) { // directory
                String dirName = in.readUTF();
                int dirChildCount = in.readInt();
                DirectoryNode dirNode = new DirectoryNode(dirName);
                parent.children.put(dirName, dirNode);

                // recurse with known childCount
                loadNodeChildren(in, dirNode, dirChildCount);
            } else if (childType == 1) { // file
                String fileName = in.readUTF();
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                FileNode fileNode = new FileNode(fileName, data);
                parent.children.put(fileName, fileNode);
            } else {
                throw new IOException("Unknown node type: " + childType);
            }
        }
    }

    private void loadNodeChildren(DataInputStream in, DirectoryNode parent, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            byte type = in.readByte();
            if (type == 0) {
                String dirName = in.readUTF();
                int dirChildCount = in.readInt();
                DirectoryNode dirNode = new DirectoryNode(dirName);
                parent.children.put(dirName, dirNode);
                loadNodeChildren(in, dirNode, dirChildCount);
            } else if (type == 1) {
                String fileName = in.readUTF();
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                FileNode fileNode = new FileNode(fileName, data);
                parent.children.put(fileName, fileNode);
            } else {
                throw new IOException("Unknown node type: " + type);
            }
        }
    }


    // ---------------- Node classes ----------------

    static abstract class Node {
        final String name;

        Node(String name) {
            this.name = name;
        }
    }

    static class DirectoryNode extends Node {
        final Map<String, Node> children = new LinkedHashMap<>();

        DirectoryNode(String name) {
            super(name);
        }
    }

    static class FileNode extends Node {
        byte[] data;

        FileNode(String name, byte[] data) {
            super(name);
            this.data = data;
        }
    }


    public static void main(String[] args) throws IOException {
        InMemoryIsolatedDirectory memDir = new InMemoryIsolatedDirectory();
        memDir.writeText("foo/bar.txt", "Hello!");
        memDir.writeText("foo/baz.txt", "World!");

        Path diskDir = Paths.get("snapshot.test");
        memDir.save(diskDir);

        InMemoryIsolatedDirectory loaded = new InMemoryIsolatedDirectory();
        loaded.load(diskDir);

        System.out.println(loaded.readText("foo/bar.txt")); // Hello!

    }

}
