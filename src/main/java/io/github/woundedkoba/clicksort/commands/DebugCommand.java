package io.github.woundedkoba.clicksort.commands;

import io.github.woundedkoba.clicksort.LanguageLoader;
import io.github.woundedkoba.dhutils.DHUtilsException;
import io.github.woundedkoba.dhutils.Debugger;
import io.github.woundedkoba.dhutils.MiscUtil;
import io.github.woundedkoba.dhutils.commands.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DebugCommand extends AbstractCommand {
    public DebugCommand() {
        super("clicksort debug", 0, 1);
        setPermissionNode("clicksort.commands.debug");
        setUsage("/<command> debug [<level>]");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        int curLevel = Debugger.getInstance().getLevel();

        if (args.length == 0) {
            Debugger.getInstance().setLevel(curLevel > 0 ? 0 : 1);
        } else {
            try {
                Debugger.getInstance().setLevel(Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                throw new DHUtilsException(LanguageLoader.getColoredMessage(
                        "invaildDebugLevel").replace("%level%", args[0]));
            }
        }

        MiscUtil.statusMessage(
                sender,
                LanguageLoader.getColoredMessage("setDebugLevelTo").replace("%level%",
                        String.valueOf(Debugger.getInstance().getLevel())));
        return true;
    }
}
