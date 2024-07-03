package io.github.woundedkoba.clicksort.commands;

import io.github.woundedkoba.clicksort.ClickSortPlugin;
import io.github.woundedkoba.clicksort.LanguageLoader;
import io.github.woundedkoba.clicksort.PlayerSortingPrefs;
import io.github.woundedkoba.clicksort.SortingMethod;
import io.github.woundedkoba.dhutils.CompatUtil;
import io.github.woundedkoba.dhutils.DHValidate;
import io.github.woundedkoba.dhutils.MiscUtil;
import io.github.woundedkoba.dhutils.commands.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ChangeSortModeCommand extends AbstractCommand {

    public ChangeSortModeCommand() {
        super("clicksort sort", 1, 1);
        setPermissionNode("clicksort.commands.sort");
        if (CompatUtil.isMaterialIdAllowed()) {
            setUsage("/clicksort sort <id|name|group|value>");
        } else {
            setUsage("/clicksort sort <name|group|value>");
        }
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        notFromConsole(sender);

        try {
            PlayerSortingPrefs prefs = ((ClickSortPlugin) plugin).getSortingPrefs();
            SortingMethod sortMethod = SortingMethod.valueOf(args[0].toUpperCase());
            DHValidate.isTrue(sortMethod.isAvailable(),
                    LanguageLoader.getColoredMessage("sortingMethodNotAvailable")
                            .replace("%method", sortMethod.toString()));
            prefs.setSortingMethod((Player) sender, sortMethod);
            MiscUtil.statusMessage(
                    sender,
                    LanguageLoader.getColoredMessage("setSortingMethodTo").replace(
                            "%method%", sortMethod.toString()));
        } catch (IllegalArgumentException e) {
            showUsage(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getEnumCompletions(sender, SortingMethod.class, args[0]);
        } else {
            return noCompletions(sender);
        }
    }
}
