ClickSortForked Change Log
-
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

ClickSortForked v0.0.2
- Added documentation to include: Change Log, Known Issues, and Planned Updates
- Upgraded to Paper API 1.21.5-R0.1-SNAPSHOT
- Upgraded to Maven Compiler Plugin 3.14.0
- Upgraded to JDBI3 3.49.5
- Upgraded to ANTLR 4 Runtime 4.13.2
- Removed adventure-platform-bukkit dependency
  - Replaced with PaperMC's Native Implementation of Kyori's Adventure API and other native functionality
- Replaced unicode escape sequences with actual characters
- Refactored the code to resolve various IDE warnings and improve overall code quality, including cleanup of redundant expressions and adherence to best practices.
- BUGFIX: Resolved an issue causing messages to have the color string prefixed to the message and are not setting the message color.
  - This involved replacing the use of legacy strings with components for messages to the player.  
    See this section from the Adventure API documentation:  
   "In Adventure, you can’t concatenate magical formatting codes. The equivalent of ChatColor in Adventure, TextColor, instead returns descriptive text describing the color when its toString() is called.  
    The recommended replacement is to convert all legacy messages to components."  
    https://docs.advntr.dev/migration/bungeecord-chat-api.html#chat-colors
    - NamedTextColor replaces TextColor 

--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
ClickSortForked v0.0.1
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