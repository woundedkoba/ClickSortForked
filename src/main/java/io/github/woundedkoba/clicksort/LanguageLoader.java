package io.github.woundedkoba.clicksort;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LanguageLoader {
    private static final String FILE_NAME = "lang.yml";

    private static ClickSortPlugin plugin;
    private static File configFile;
    private static FileConfiguration config;

    public static void init(ClickSortPlugin plugin) {
        LanguageLoader.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), FILE_NAME);

        saveDefault();
        load();
    }

    public static void reload() {
        init(plugin);
    }

    public static void saveDefault() {
        // Prevent Bukkit from giving out warnings
        if (!configFile.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
    }

    public static void load() {
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultConfigStream = plugin.getResource(FILE_NAME);

        if (defaultConfigStream != null) {
            InputStreamReader configReader = new InputStreamReader(defaultConfigStream);
            YamlConfiguration defaultConfig = YamlConfiguration
                    .loadConfiguration(configReader);
            config.setDefaults(defaultConfig);
        }
    }

    public static String getMessage(String path) {
        return config.getString(path);
    }

    public static String getMessage(String path, String def) {
        String msg = config.getString(path);
        return msg != null ? msg : def;
    }

    public static String getColoredMessage(String path) {
        return getColoredString(getMessage(path));
    }

    public static String getColoredMessage(String path, String def) {
        String colorMsg = getColoredString(config.getString(path));
        return colorMsg != null ? colorMsg : def;
    }

    private static String getColoredString(String str) {
        if (str == null) {
            return null;
        }
        // De-serialize the string with '&' as the color code character
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(str);
        // Serialize the component back to a legacy string using the standard color code character
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
