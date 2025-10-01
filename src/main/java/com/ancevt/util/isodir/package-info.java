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

/**
 * Provides sandboxed directory utilities for safe file I/O.
 *
 * <p>
 * This package contains classes for working with file systems in a safe,
 * isolated manner:
 * </p>
 *
 * <ul>
 *   <li>{@link com.ancevt.util.isodir.IsolatedDirectory} — a wrapper around
 *   the filesystem that prevents path traversal outside a configured base
 *   directory and provides convenient read/write operations.</li>
 *
 *   <li>{@link com.ancevt.util.isodir.InMemoryIsolatedDirectory} — an
 *   in-memory implementation useful for testing or ephemeral storage, with
 *   optional JSON serialization.</li>
 *
 *   <li>{@link com.ancevt.util.isodir.OsUtils} — helpers for detecting the
 *   current operating system and resolving application data paths.</li>
 *
 *   <li>{@link com.ancevt.util.isodir.IsolatedDirectoryException} — runtime
 *   exception type for all failures in this package.</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 *
 * <pre>{@code
 * IsolatedDirectory dir = new IsolatedDirectory("data");
 * dir.writeText("config/settings.txt", "volume=80");
 * String text = dir.readText("config/settings.txt");
 * }</pre>
 *
 * <p>
 * By using these classes, applications can safely manage file operations
 * without the risk of accidental or malicious directory traversal.
 * </p>
 */
package com.ancevt.util.isodir;
