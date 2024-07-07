ClickSortForked is a Paper plugin forked from ChengZi's ClickSort 1.6.1.

ClickSort makes it very easy for players to sort their inventories (player/chest/dispenser) with a single or double click of the mouse. 
Players can sort by item ID, item name, switch between single- and double-clicking, or disable sorting entirely, all on a per-player basis and without needing any commands; it's all done with mouse clicks in inventory windows.

ClickSortForked 0.0.1 was created because Minecraft 1.20.5, 1.20.6, and 1.21 broke how ChengZi's ClickSort 1.6.1 was handling version control.
Starting with Minecraft 1.20.5 Java 21 is required. I assume this to be what caused the breaking issue.

Without going into full detail changes include but are not limited to:
- Upgraded to Java 21
- Upgraded to Paper API 1.21-R0.1-SNAPSHOT (ChengZi's ClickSort 1.6.1 uses the Spigot API)
- Upgraded to JDBI3 3.45.1
- Upgraded to ANTLR 4 Runtime 4.13.1
- Upgraded to Maven Compiler Plugin 3.13.0
- Upgraded to Maven Shade Plugin 3.6.0
- Upgraded to Maven Resources Plugin 3.3.1 (not sure what version was used in ChengZi's ClickSort 1.6.1)
- Removed the use of Maven Snapshots
- Removed the use of bStats and Metrics
- Implemented the use of the Adventure Platform API (to replace the use of ChatColor from the Spigot API as this is deprecated for the Paper API)
- BUGFIX: Re-wrote how CompatUtil acquires the Minecraft Sub Version (This was causing the plugin to fail to enable for MC 1.20.5, 1.20.6, and 1.21)
