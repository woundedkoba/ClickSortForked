package io.github.woundedkoba.clicksort.commands;

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
import io.github.woundedkoba.clicksort.LanguageLoader;
import io.github.woundedkoba.dhutils.MiscUtil;
import io.github.woundedkoba.dhutils.commands.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import io.github.woundedkoba.clicksort.util.LocalUtil;

public class ReloadCommand extends AbstractCommand {

    public ReloadCommand() {
        super("clicksort reload", 0, 0);
        setPermissionNode("clicksort.commands.reload");
        setUsage("/clicksort reload");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        ClickSortPlugin csPlugin = (ClickSortPlugin) plugin;
        csPlugin.reloadConfig();
        csPlugin.processConfig();
        csPlugin.getItemGrouping().load();
        csPlugin.getItemValues().load();
        LanguageLoader.reload();
        LocalUtil.reload(plugin);
        MiscUtil.statusMessage(sender, LanguageLoader.getColoredMessage("configReloaded"));

        return true;
    }

}
