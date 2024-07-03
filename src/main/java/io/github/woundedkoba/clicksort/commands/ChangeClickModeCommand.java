package io.github.woundedkoba.clicksort.commands;

import io.github.woundedkoba.clicksort.ClickMethod;
import io.github.woundedkoba.clicksort.ClickSortPlugin;
import io.github.woundedkoba.clicksort.LanguageLoader;
import io.github.woundedkoba.clicksort.PlayerSortingPrefs;
import io.github.woundedkoba.dhutils.CompatUtil;
import io.github.woundedkoba.dhutils.DHValidate;
import io.github.woundedkoba.dhutils.MiscUtil;
import io.github.woundedkoba.dhutils.commands.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ChangeClickModeCommand extends AbstractCommand {

    public ChangeClickModeCommand() {
        super("clicksort click", 1, 1);
        setPermissionNode("clicksort.commands.click");
        String usage = "/clicksort click <single|double";
        if (CompatUtil.isMiddleClickAllowed()) {
            usage += "|middle";
        }
        if (CompatUtil.isSwapKeyAvailable()) {
            usage += "|swap";
        }
        setUsage(usage + "|none>");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        notFromConsole(sender);

        try {
            PlayerSortingPrefs prefs = ((ClickSortPlugin) plugin).getSortingPrefs();
            ClickMethod clickMethod = ClickMethod.valueOf(args[0].toUpperCase());
            DHValidate.isTrue(clickMethod.isAvailable(),
                    LanguageLoader.getColoredMessage("clickMethodNotAvailable")
                            .replace("%method", clickMethod.toString()));
            prefs.setClickMethod((Player) sender, clickMethod);
            MiscUtil.statusMessage(
                    sender,
                    LanguageLoader.getColoredMessage("setClickMethodTo").replace(
                            "%method%", clickMethod.toString()));
        } catch (IllegalArgumentException e) {
            showUsage(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getEnumCompletions(sender, ClickMethod.class, args[0]);
        } else {
            return noCompletions(sender);
        }
    }
}
