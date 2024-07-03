package io.github.woundedkoba.dhutils;

import org.bukkit.Bukkit;

public class CompatUtil {
	public static int GetMinecraftSubVersion() {
        String minecraftVersion = Bukkit.getServer().getBukkitVersion().split("-")[0];
        String[] versionParts = minecraftVersion.split("\\.");

        if (versionParts.length < 2) {
            // Handle the case where the version format is unexpected
            return 0; // Or any default value you prefer
        }

        String subVersion = versionParts[1];
        return Integer.parseInt(subVersion);
    }

    public static boolean isMaterialIdAllowed() {
        return GetMinecraftSubVersion() <= 12;
    }

    public static boolean isMiddleClickAllowed() {
        return GetMinecraftSubVersion() <= 17;
    }

    public static boolean isSwapKeyAvailable() {
        return GetMinecraftSubVersion() >= 16;
    }
}
