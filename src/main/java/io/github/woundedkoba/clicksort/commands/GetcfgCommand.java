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

import io.github.woundedkoba.dhutils.MessagePager;
import io.github.woundedkoba.dhutils.commands.AbstractCommand;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class GetcfgCommand extends AbstractCommand {
    public GetcfgCommand() {
        super("clicksort getcfg");
        setPermissionNode("clicksort.commands.getcfg");
        setUsage("/<command> getcfg");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender commandSender, String[] strings) {
        MessagePager pager = MessagePager.getPager(commandSender).clear()
                .setParseColours(true);
        for (String key : plugin.getConfig().getKeys(true)) {
            if (!plugin.getConfig().isConfigurationSection(key)) {
                pager.add(NamedTextColor.WHITE + key + " = " + NamedTextColor.YELLOW
                        + plugin.getConfig().get(key));
            }
        }
        pager.showPage();
        return true;
    }
}
