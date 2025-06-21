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
                // Стандарт XDG, если не задан XDG_DATA_HOME — ~/.local/share
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

    private OsUtils() {} // Utility class, no instance.
}
