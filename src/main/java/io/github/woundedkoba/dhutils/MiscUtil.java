package io.github.woundedkoba.dhutils;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MiscUtil {
    private static final Map<String, NamedTextColor> prevColours = new HashMap<>();

    public static final NamedTextColor STATUS_COLOUR = NamedTextColor.AQUA;
    public static final NamedTextColor ERROR_COLOUR = NamedTextColor.RED;
    public static final NamedTextColor ALERT_COLOUR = NamedTextColor.YELLOW;
    public static final NamedTextColor GENERAL_COLOUR = NamedTextColor.WHITE;

    private static final Component BROADCAST_PREFIX = Component.text("✱ ", NamedTextColor.RED);
    private static boolean colouredConsole = true;

    public static void setColouredConsole(boolean coloured) {
        colouredConsole = coloured;
    }

    // ======================== Message Methods ========================

    public static void errorMessage(CommandSender sender, String string) {
        setPrevColour(sender.getName(), ERROR_COLOUR);
        message(sender, Component.text(string, ERROR_COLOUR), Level.WARNING);
        prevColours.remove(sender.getName());
    }

    public static void statusMessage(CommandSender sender, String string) {
        setPrevColour(sender.getName(), STATUS_COLOUR);
        message(sender, Component.text(string, STATUS_COLOUR), Level.INFO);
        prevColours.remove(sender.getName());
    }

    public static void alertMessage(CommandSender sender, String string) {
        setPrevColour(sender.getName(), ALERT_COLOUR);
        message(sender, Component.text(string, ALERT_COLOUR), Level.INFO);
        prevColours.remove(sender.getName());
    }

    public static void generalMessage(CommandSender sender, String string) {
        setPrevColour(sender.getName(), GENERAL_COLOUR);
        message(sender, Component.text(string, GENERAL_COLOUR), Level.INFO);
        prevColours.remove(sender.getName());
    }

    public static void broadcastMessage(String string) {
        // Broadcast to all players (and console) using Components and color.
        Component messageComponent = BROADCAST_PREFIX.append(Component.text(string, NamedTextColor.YELLOW));
        Bukkit.broadcast(messageComponent);
    }

    // ======================== Utility Methods ========================

    private static void setPrevColour(String name, NamedTextColor colour) {
        prevColours.put(name, colour);
    }

    private static NamedTextColor getPrevColour(String name) {
        return prevColours.getOrDefault(name, GENERAL_COLOUR);
    }

    public static void rawMessage(CommandSender sender, String string) {
        // Send each line as a plain message (without color formatting if needed)
        boolean strip = sender instanceof ConsoleCommandSender && !colouredConsole;
        for (String line : string.split("\\n")) {
            if (strip) {
                sender.sendMessage(LegacyComponentSerializer.legacySection()
                        .deserialize(line)
                        .content()); // Just send plain text
            } else {
                sender.sendMessage(Component.text(line));
            }
        }
    }

    private static void message(CommandSender sender, Component message, Level level) {
        boolean strip = sender instanceof ConsoleCommandSender && !colouredConsole;
        if (strip) {
            // Log plain message (strip color)
            String plain = PlainTextComponentSerializer.plainText().serialize(message);
            LogUtils.log(level, plain);
        } else {
            // Modern: send as Component (uses color)
            sender.sendMessage(message);
        }
    }

    public static String formatLocation(Location loc) {
        return String.format("%d,%d,%d,%s", loc.getBlockX(), loc.getBlockY(),
                loc.getBlockZ(), loc.getWorld().getName());
    }

    public static Location parseLocation(String arglist) {
        return parseLocation(arglist, null);
    }

    public static Location parseLocation(String arglist, CommandSender sender) {
        String s = sender instanceof Player ? "" : ",worldname";
        String[] args = arglist.split(",");

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            World w = (sender instanceof Player) ? ((Player) sender).getWorld()
                    : findWorld(args[3]);
            return new Location(w, x, y, z);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("You must specify all of x,y,z" + s + ".");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in " + arglist);
        }
    }

    private static final Pattern colourPat = Pattern
            .compile("(?<!&)&(?=[0-9a-fA-Fk-oK-OrR])");

    // --------- LEGACY UTILS FOR CONFIG SUPPORT ---------

    /**
     * Converts &-color-coded strings from config to Components.
     */
    public static Component parseColourSpec(String spec) {
        return parseColourSpec(null, spec);
    }

    public static Component parseColourSpec(CommandSender sender, String spec) {
        if (spec == null) return Component.empty();
        String who = sender == null ? "*" : sender.getName();
        String res = colourPat.matcher(spec).replaceAll("§");
        // Optionally restore "previous color" codes or similar, if needed.
        res = res.replace("&-", getPrevColour(who).toString()).replace("&&", "&");
        return LegacyComponentSerializer.legacySection().deserialize(res);
    }

    public static String unParseColourSpec(String spec) {
        return spec.replaceAll("§", "&");
    }

    /**
     * Find the given world by name.
     */
    public static World findWorld(String worldName) {
        World w = Bukkit.getServer().getWorld(worldName);
        if (w != null) {
            return w;
        } else {
            throw new IllegalArgumentException("World " + worldName
                    + " was not found on the server.");
        }
    }

    // ... Other unchanged utility methods below ...

    public static List<String> splitQuotedString(String s) {
        List<String> matchList = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(s);

        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                matchList.add(regexMatcher.group(2));
            } else {
                matchList.add(regexMatcher.group());
            }
        }
        return matchList;
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        Collections.sort(list);
        return list;
    }

    public static <T> List<T>[] splitList(List<T> list, int nLists) {
        @SuppressWarnings("unchecked")
        List<T>[] res = new ArrayList[nLists];
        Collections.shuffle(list);
        for (int i = 0; i < list.size(); i++) {
            res[i % nLists].add(list.get(i));
        }
        return res;
    }

    public static String[] listFilesinJAR(File jarFile, String path, String ext)
            throws IOException {
        ZipInputStream zip = new ZipInputStream(new FileInputStream(jarFile));
        ZipEntry ze;

        List<String> list = new ArrayList<>();
        while ((ze = zip.getNextEntry()) != null) {
            String entryName = ze.getName();
            if (entryName.startsWith(path) && ext != null && entryName.endsWith(ext)) {
                list.add(entryName);
            }
        }
        zip.close();
        return list.toArray(new String[list.size()]);
    }

    public static YamlConfiguration loadYamlUTF8(File file)
            throws InvalidConfigurationException, IOException {
        StringBuilder sb = new StringBuilder((int) file.length());

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(
                file), StandardCharsets.UTF_8));
        char[] buf = new char[1024];
        int l;
        while ((l = in.read(buf, 0, buf.length)) > -1) {
            sb = sb.append(buf, 0, l);
        }
        in.close();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(sb.toString());
        return yaml;
    }

    public static boolean looksLikeUUID(String s) {
        return s.length() == 36 && s.charAt(8) == '-' && s.charAt(13) == '-'
                && s.charAt(18) == '-' && s.charAt(23) == '-';
    }
}