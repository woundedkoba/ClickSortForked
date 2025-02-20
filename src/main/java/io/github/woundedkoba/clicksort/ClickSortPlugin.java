package io.github.woundedkoba.clicksort;

/*
 * This file is part of ClickSort
 *
 * ClickSort is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * ClickSort is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with ClickSort. If not, see <http://www.gnu.org/licenses/>.
 */

import io.github.woundedkoba.clicksort.commands.*;
import io.github.woundedkoba.clicksort.events.InventorySortEvent;
import io.github.woundedkoba.dhutils.*;
import io.github.woundedkoba.dhutils.commands.CommandManager;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import io.github.woundedkoba.clicksort.util.LocalUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ClickSortPlugin extends JavaPlugin implements Listener {
    private final CommandManager cmds = new CommandManager(this);
    private final CooldownMessager messager = new CooldownMessager();
    private PlayerSortingPrefs sortingPrefs;
    private BukkitTask purgeTask;
    private ItemGrouping itemGroups;
    private ItemValues itemValues;
    private List<InventoryType> sortableInventories;

    private static ClickSortPlugin instance = null;

    @Override
    public void onEnable() {
        instance = this;

        LogUtils.init(this);
        LanguageLoader.init(this);

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(this, this);

        getConfig().options().setHeader(List.of("See https://dev.bukkit.org/projects/clicksort/pages/configuration"));
        getConfig().options().copyDefaults(true);
        getConfig().set("log_level", null); // superseded by debug_level
        getConfig().set("autosave_seconds", null); // superseded by autopurge_seconds
        saveConfig();

        Debugger.getInstance().setPrefix("[ClickSort] ");
        Debugger.getInstance().setLevel(getConfig().getInt("debug_level"));
        Debugger.getInstance().setTarget(getServer().getConsoleSender());

        cmds.registerCommand(new ChangeClickModeCommand());
        cmds.registerCommand(new ChangeSortModeCommand());
        cmds.registerCommand(new DebugCommand());
        cmds.registerCommand(new GetcfgCommand());
        cmds.registerCommand(new ReloadCommand());
        cmds.registerCommand(new ShiftClickCommand());

        sortingPrefs = new PlayerSortingPrefs(this);
        sortingPrefs.load();

        itemGroups = new ItemGrouping(this);
        itemGroups.load();
        itemValues = new ItemValues(this);
        itemValues.load();

        LocalUtil.init(this);

        processConfig();
    }

    @Override
    public void onDisable() {
        if (purgeTask != null) {
            purgeTask.cancel();
        }
        LocalUtil.save();

        instance = null;
    }

    public static ClickSortPlugin getInstance() {
        return instance;
    }

    public CooldownMessager getMessager() {
        return messager;
    }

    public Path saveDefaultResource(String resourcePath) {
        Path path = getDataFolder().toPath().resolve(resourcePath);
        if (!path.toFile().exists()) {
            saveResource(resourcePath, false);
        }
        return path;
    }

    /**
     * @return the sorting
     */
    public PlayerSortingPrefs getSortingPrefs() {
        return sortingPrefs;
    }

    public ItemGrouping getItemGrouping() {
        return itemGroups;
    }

    public ItemValues getItemValues() {
        return itemValues;
    }

    public SortingMethod getDefaultSortingMethod() {
        return SortingMethod.parse(getConfig().getString("defaults.sort_mode"), SortingMethod.preferredDefault());
    }

    public ClickMethod getDefaultClickMethod() {
        return ClickMethod.parse(getConfig().getString("defaults.click_mode"), ClickMethod.preferredDefault());
    }

    public boolean getDefaultShiftClick() {
        return getConfig().getBoolean("defaults.shift_click");
    }

    /**
     * Inventory click handler. Run with priority HIGHEST - this makes it run late, giving protection plugins a chance to cancel the inventory click
     * event first.
     *
     * @param event the event object
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClicked(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        if (!PermissionUtils.isAllowedTo(player, "clicksort.sort")) {
            return;
        }

        String playerName = player.getName();

        Debugger.getInstance().debug("inventory click by player " + playerName + ": type=" + event.getClick() + " slot="
                + event.getSlot() + " rawslot=" + event.getRawSlot());

        SortingMethod sortMethod = sortingPrefs.getSortingMethod(player);
        ClickMethod clickMethod = sortingPrefs.getClickMethod(player);
        boolean allowShiftClick = sortingPrefs.getShiftClickAllowed(player);

        if (event.getCurrentItem().getType() == Material.AIR && event.isShiftClick() && allowShiftClick) {
            if (event.isLeftClick() && clickMethod != ClickMethod.NONE) {
                // shift-left-clicking an empty slot cycles sort method for the player
                do {
                    sortMethod = sortMethod.next();
                } while (!sortMethod.isAvailable());
                sortingPrefs.setSortingMethod(player, sortMethod);
                MiscUtil.statusMessage(player,
                        LanguageLoader.getColoredMessage("sortBy").replace("%method%", sortMethod.toString())
                                .replace("%instruction%", clickMethod.getInstruction()));
                // Create the Component with formatting for "shiftLeftToChange"
                Component messageComponent = Component.text(LanguageLoader.getColoredMessage("shiftLeftToChange"))
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true);
                // Serialize the Component to a legacy string
                String legacyMessage = LegacyComponentSerializer.legacySection().serialize(messageComponent);
                // Use the legacy formatted string in the message method
                messager.message(player, "leftclick", 60, legacyMessage);
            } else if (event.isRightClick()) {
                // shift-right-clicking an empty slot cycles click method for the player
                clickMethod = clickMethod.nextAvailable();
                sortingPrefs.setClickMethod(player, clickMethod);
                MiscUtil.statusMessage(player, clickMethod.getInstruction());
                // Create the Component with formatting for "shiftRightToChange"
                Component rightClickMessageComponent = Component.text(LanguageLoader.getColoredMessage("shiftRightToChange"))
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true);
                // Serialize the Component to a legacy string
                String rightClickLegacyMessage = LegacyComponentSerializer.legacySection().serialize(rightClickMessageComponent);
                // Use the legacy formatted string in the message method
                messager.message(player, "rightclick", 60, rightClickLegacyMessage);
            }
            return;
        }

        boolean shouldSort = switch (clickMethod) {
            case SINGLE -> event.getClick() == ClickType.LEFT && event.getCurrentItem().getType() == Material.AIR && (
                    event.getCursor() == null || event.getCursor().getType() == Material.AIR);
            case DOUBLE -> event.getClick() == ClickType.DOUBLE_CLICK;
            case MIDDLE -> event.getClick() == ClickType.MIDDLE;
            case SWAP -> event.getClick() == ClickType.SWAP_OFFHAND;
            default -> false;
        };
        if (shouldSort && shouldSort(viewToClickedInventory(event.getView(), event.getRawSlot()))) {
            if (sortInventory(event, sortMethod) && clickMethod.shouldCancelEvent()) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    player.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
                }, 1L);
                event.setCancelled(true);
            }
        }
    }

    private Inventory viewToClickedInventory(InventoryView view, int rawSlot) {
        return rawSlot < 0 ? null
                : (rawSlot < view.getTopInventory().getSize() ? view.getTopInventory() : view.getBottomInventory());
    }

    private boolean shouldSort(Inventory clickedInventory) {
        return clickedInventory != null && !shouldIgnore(clickedInventory) && sortableInventories.contains(
                clickedInventory.getType());
    }

    private boolean shouldIgnore(Inventory inventory) {
        return getConfig().getBoolean("ignore_plugin_inventory") && !isVanillaInventoryHolder(inventory.getHolder());
    }

    private static boolean isVanillaInventoryHolder(InventoryHolder inventoryHolder) {
        return inventoryHolder != null && inventoryHolder.getClass().getPackageName().startsWith("org.bukkit.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return cmds.dispatch(sender, command, label, args);
        } catch (DHUtilsException e) {
            MiscUtil.errorMessage(sender, e.getMessage());
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return cmds.onTabComplete(sender, command, label, args);
    }

    public void processConfig() {
        setupPurgeTask();

        MiscUtil.setColouredConsole(getConfig().getBoolean("coloured_console"));

        Debugger.getInstance().setLevel(getConfig().getInt("debug_level"));

        sortableInventories = getConfig().getStringList("sortable_inventories").stream().map(s -> {
            try {
                return InventoryType.valueOf(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Purge unseen player sorting data periodically if necessary
     */
    private void setupPurgeTask() {
        if (purgeTask != null) {
            purgeTask.cancel();
            purgeTask = null;
        }

        int period = getConfig().getInt("autopurge_seconds");
        if (period > 0) {
            purgeTask = getServer().getScheduler()
                    .runTaskTimerAsynchronously(this, () -> sortingPrefs.purge(), 0L, 20L * period);
        }
    }

    private boolean sortInventory(final InventoryClickEvent event, final SortingMethod sortMethod) {
        Player p = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();
        int slot = event.getView().convertSlot(rawSlot);

        Inventory inv;
        if (slot == rawSlot) {
            // upper inv was clicked
            inv = event.getView().getTopInventory();
            if (slot >= inv.getSize()) {
                // is this a Bukkit bug? clicking a player inventory when the
                // crafting or dispenser view is up
                // seems to give rawSlot==localSlot, implying the upper
                // inventory (crafting/dispenser) has been clicked
                // when in fact the lower inventory (player) was clicked
                inv = event.getView().getBottomInventory();
            }
        } else {
            // lower inv was clicked
            inv = event.getView().getBottomInventory();
        }

        Debugger.getInstance().debug("clicked inventory window " + inv.getType() + ", slot " + slot);
        int min, max; // slot range to sort
        InventoryType type = inv.getType();
        if (type == InventoryType.PLAYER) {
            if (slot < 9) {
                // hotbar
                if (!PermissionUtils.isAllowedTo(p, "clicksort.sort.hotbar")) {
                    return false;
                }
                min = 0;
                max = 9;
            } else {
                if (!PermissionUtils.isAllowedTo(p, "clicksort.sort.player")) {
                    return false;
                }
                // main player inventory
                min = getConfig().getInt("player_sort_min");
                // don't sort equipments and off-hand
                max = getConfig().getInt("player_sort_max");
            }
        } else if (sortableInventories.contains(type)) {
            if (!PermissionUtils.isAllowedTo(p, "clicksort.sort.container")) {
                return false;
            }
            min = inv.getHolder() instanceof AbstractHorse ? 2 : 0;
            max = inv.getSize();
        } else {
            return false;
        }

        InventorySortEvent sortEvent = new InventorySortEvent(event.getView(), inv, min, max);
        Bukkit.getPluginManager().callEvent(sortEvent);
        if (sortEvent.isCancelled()) {
            return false;
        }

        Set<Integer> sortableSlots = sortEvent.getSortableSlots();
        List<ItemStack> sortedItems = sortAndMerge(inv.getContents(), sortableSlots, sortMethod);

        if (sortableSlots.size() < sortedItems.size() && !getConfig().getBoolean("drop_excess")) {
            MiscUtil.errorMessage(p, LanguageLoader.getColoredMessage("invOverFlow"));
            return false;
        }

        for (int i : sortableSlots) {
            if (!sortedItems.isEmpty()) {
                ItemStack newItem = sortedItems.remove(0);
                inv.setItem(i, newItem);
            } else {
                inv.clear(i);
            }
        }

        if (!sortedItems.isEmpty()) {
            // This *shouldn't* happen, but there is a possibility if some other plugin has been messing
            // with max stack sizes, and we end up with an overflowing inventory after merging stacks.
            MiscUtil.alertMessage(p, LanguageLoader.getColoredMessage("dropItems"));
            for (ItemStack item : sortedItems) {
                Debugger.getInstance().debug("dropping " + item + " by player " + p.getName());
                p.getWorld().dropItemNaturally(p.getLocation(), item);
            }
        }

        for (HumanEntity he : event.getViewers()) {
            if (he instanceof Player viewer) {
                viewer.updateInventory();
            }
        }

        return true;
    }

    private List<ItemStack> sortAndMerge(ItemStack[] items, Set<Integer> sortableSlots, SortingMethod sortMethod) {
        Map<SortKey, Integer> amounts = new HashMap<>();

        // phase 1: extract a list of unique material/data/item-meta strings and
        // use those as keys
        // into a hash which maps items to quantities
        Debugger.getInstance().debug("sortAndMerge: sortable = " + sortableSlots + ", size = " + items.length);
        for (int i : sortableSlots) {
            ItemStack is = items[i];
            if (is != null) {
                SortKey key = new SortKey(is, sortMethod);
                if (amounts.containsKey(key)) {
                    amounts.put(key, amounts.get(key) + is.getAmount());
                } else {
                    amounts.put(key, is.getAmount());
                }
            }
        }

        // Sanity check
        checkNoNulls(amounts, items);

        // phase 2: sort the extracted item keys and reconstruct the item stacks
        // from those keys
        List<ItemStack> sorted = new LinkedList<>();
        for (SortKey sortKey : MiscUtil.asSortedList(amounts.keySet())) {
            int amount = amounts.get(sortKey);
            Debugger.getInstance().debug(2, "Process item [" + sortKey + "], amount = " + amount);
            Material mat = sortKey.getMaterial();
            int maxStack = mat.getMaxStackSize();
            Debugger.getInstance().debug(2, "max stack size for " + mat + " = " + maxStack);
            if (maxStack != 0) {
                while (amount > maxStack) {
                    sorted.add(sortKey.toItemStack(maxStack));
                    amount -= maxStack;
                }
                sorted.add(sortKey.toItemStack(amount));
            }
        }

        return sorted;
    }

    private void checkNoNulls(Map<SortKey, Integer> amounts, ItemStack[] items) {
        for (SortKey key : amounts.keySet()) {
            if (key == null) {
                LogUtils.severe("Detected null sort key!  Inventory dump follows:");
                for (ItemStack item : items) {
                    LogUtils.severe(item.toString());
                }
                LogUtils.severe(
                        "Please report this, quoting all above error text, in a ticket at https://github.com/NewbieOrange/clicksort/issues/");
            }
        }
    }
}
