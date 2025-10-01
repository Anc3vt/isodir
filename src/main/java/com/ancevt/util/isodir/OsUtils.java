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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

final class OsUtils {

    public enum OperatingSystem {
        WINDOWS, MACOS, UNIX_LIKE, SOLARIS, FREEBSD, HPUX, OTHER
    }

    public static OperatingSystem getOperatingSystem() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) return OperatingSystem.WINDOWS;
        if (osName.contains("mac")) return OperatingSystem.MACOS;
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
            return OperatingSystem.UNIX_LIKE;
        if (osName.contains("sunos")) return OperatingSystem.SOLARIS;
        if (osName.contains("freebsd")) return OperatingSystem.FREEBSD;
        if (osName.contains("hp-ux")) return OperatingSystem.HPUX;
        return OperatingSystem.OTHER;
    }

    public static Path getApplicationDataPath() {
        OperatingSystem os = getOperatingSystem();
        String userHome = System.getProperty("user.home");

        switch (os) {
            case WINDOWS:
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null) return Paths.get(localAppData);
                String appData = System.getenv("APPDATA");
                if (appData != null) return Paths.get(appData);
                return Paths.get(userHome, "AppData", "Local");
            case MACOS:
                return Paths.get(userHome, "Library", "Application Support");
            case UNIX_LIKE:
                String xdgDataHome = System.getenv("XDG_DATA_HOME");
                if (xdgDataHome != null) return Paths.get(xdgDataHome);
                return Paths.get(userHome, ".local", "share");
            case SOLARIS:
            case FREEBSD:
            case HPUX:
            case OTHER:
            default:
                return Paths.get(userHome);
        }
    }

    private OsUtils() {} 
}
