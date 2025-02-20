package io.github.woundedkoba.clicksort.util;

/*
 This file is part of ClickSort

 ClickSort is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 ClickSort is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with ClickSort.  If not, see <http://www.gnu.org/licenses/>.
 */

import io.github.woundedkoba.clicksort.ClickSortPlugin;
import io.github.woundedkoba.dhutils.JARUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

public final class LocalUtil {
    private static final String CONFIG_NAME = "items.yml";
    public static FileConfiguration config;
    public static File file;
    public static Logger log = ClickSortPlugin.getInstance().getLogger();

    public static String getItemFullName(final ItemStack i) {
        final String name = getItemName(getItemType(i));
        if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
            return name + " (" + i.getItemMeta().displayName() + ")";
        }
        return name;
    }

    public static String getItemName(final ItemStack i) {
        if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
        	Component displayName = i.getItemMeta().displayName();
        	/// Convert Component to String
            return Component.text().content(displayName.toString()).toString();
        }
        return getItemName(getItemType(i));
    }

    public static String getItemName(final String iname) {
        if (config == null) {
            return iname;
        }
        String aname = config.getString(iname);
        if (aname == null) {
            aname = iname;
            config.set(iname, iname);
        }
        return aname;
    }

    public static String getItemType(final ItemStack i) {
        return i.getType().name().toUpperCase(Locale.ROOT);
    }

    public static void init(final Plugin plugin) {
        log = plugin.getLogger();
        if (config == null) {
            new JARUtil(plugin).extractResource(CONFIG_NAME, plugin.getDataFolder());
            config = YamlConfiguration.loadConfiguration(file = new File(plugin.getDataFolder(), CONFIG_NAME));
        }
    }

    public static boolean isInitialized() {
        return config != null;
    }

    public static void reload(final Plugin plugin) {
        new JARUtil(plugin).extractResource(CONFIG_NAME, plugin.getDataFolder());
        config = YamlConfiguration.loadConfiguration(file = new File(plugin.getDataFolder(), CONFIG_NAME));
    }


    public static void save() {
        if (isInitialized()) {
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
