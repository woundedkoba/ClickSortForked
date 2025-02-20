package io.github.woundedkoba.dhutils.commands;

import com.google.common.base.Joiner;
import io.github.woundedkoba.dhutils.DHUtilsException;
import io.github.woundedkoba.dhutils.Debugger;
import io.github.woundedkoba.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author des
 */
public abstract class AbstractCommand implements Comparable<AbstractCommand> {
    private enum OptType {
        BOOL, STRING, INT, DOUBLE
    }

    private final int minArgs, maxArgs;
    private final List<CommandRecord> cmdRecs = new ArrayList<AbstractCommand.CommandRecord>();
    private final Map<String, OptType> options = new HashMap<String, OptType>();
    private final Map<String, Object> optVals = new HashMap<String, Object>();
    private String[] usage;
    private String permissionNode;
    private boolean quotedArgs;
    private CommandRecord matchedCommand;
    private String[] matchedArgs;

    public AbstractCommand(String label) {
        this(label, 0, Integer.MAX_VALUE);
    }

    public AbstractCommand(String label, int minArgs) {
        this(label, minArgs, Integer.MAX_VALUE);
    }

    public AbstractCommand(String label, int minArgs, int maxArgs) {
        quotedArgs = false;

        setUsage("");
        addAlias(label);
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    public abstract boolean execute(Plugin plugin, CommandSender sender, String[] args);

    public void addAlias(String label) {
        String[] fields = label.split(" ");
        cmdRecs.add(new CommandRecord(fields));
    }

    public boolean matchesSubCommand(String label, String[] args) {
        return matchesSubCommand(label, args, false);
    }

    public boolean matchesSubCommand(String label, String[] args, boolean partialOk) {
        CMDREC:
        for (CommandRecord rec : cmdRecs) {
            if (!label.equalsIgnoreCase(rec.getCommand())) {
                continue;
            }

            if (!partialOk && args.length < rec.subCommands.length) {
                continue;
            }

            int match = 0;
            for (int i = 0; i < rec.subCommands.length && i < args.length; i++) {
                if (rec.subCommands[i].startsWith(args[i])) {
                    match++;
                } else {
                    // match failed; try the next command record, if any
                    continue CMDREC;
                }
            }
            if (partialOk || match == rec.subCommands.length) {
                matchedCommand = rec;
                return true;
            }
        }

        return false;
    }

    public boolean matchesArgCount(String label, String[] args) {
        for (CommandRecord rec : cmdRecs) {
            if (!label.equalsIgnoreCase(rec.getCommand())) {
                continue;
            }

            int nArgs;
            if (isQuotedArgs()) {
                List<String> a = MiscUtil.splitQuotedString(combine(args, 0));
                nArgs = a.size() - rec.subCommands.length;
            } else {
                nArgs = args.length - rec.subCommands.length;
            }
            Debugger.getInstance().debug(
                    3,
                    String.format("matchesArgCount: %s, nArgs=%d min=%d max=%d", label,
                            nArgs, minArgs, maxArgs));
            if (nArgs >= minArgs && nArgs <= maxArgs) {
                storeMatchedArgs(args, rec);
                return true;
            }
        }
        matchedArgs = null;
        return false;
    }

    List<CommandRecord> getCmdRecs() {
        return cmdRecs;
    }

    private void storeMatchedArgs(String[] args, CommandRecord rec) {
        String[] tmpResult = new String[args.length - rec.subCommands.length];
        for (int i = rec.subCommands.length; i < args.length; i++) {
            tmpResult[i - rec.subCommands.length] = args[i];
        }

        String[] tmpArgs;
        if (isQuotedArgs()) {
            List<String> a = MiscUtil.splitQuotedString(combine(tmpResult, 0));
            tmpArgs = a.toArray(new String[a.size()]);
        } else {
            tmpArgs = tmpResult;
        }

        // extract any command-line options that were specified
        List<String> l = new ArrayList<String>(tmpArgs.length);
        optVals.clear();
        for (int i = 0; i < tmpArgs.length; i++) {
            String opt;
            if (tmpArgs[i].length() < 2 || !tmpArgs[i].startsWith("-")) {
                opt = "";
            } else {
                opt = tmpArgs[i].substring(1);
            }
            if (options.containsKey(opt)) {
                try {
                    switch (options.get(opt)) {
                        case BOOL:
                            optVals.put(opt, true);
                            break;
                        case STRING:
                            optVals.put(opt, tmpArgs[++i]);
                            break;
                        case INT:
                            optVals.put(opt, Integer.parseInt(tmpArgs[++i]));
                            break;
                        case DOUBLE:
                            optVals.put(opt, Double.parseDouble(tmpArgs[++i]));
                            break;
                        default:
                            throw new IllegalStateException("unexpected option type for "
                                    + tmpArgs[i]);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new DHUtilsException("Missing value for option '"
                            + tmpArgs[i - 1] + "'");
                } catch (Exception e) {
                    throw new DHUtilsException("Invalid value for option '"
                            + tmpArgs[i - 1] + "'");
                }
            } else {
                l.add(tmpArgs[i]);
            }
        }

        matchedArgs = l.toArray(new String[l.size()]);
    }

    protected void showUsage(CommandSender sender, String alias, String prefix) {
        if (usage.length == 0) {
            return;
        }
        String indent;
        if (prefix == null || prefix.isEmpty()) {
            indent = "";
        } else {
            int l = prefix.length();
            indent =  " ".repeat(sender instanceof Player ? l + 2 : l);
        }

        for (int i = 0; i < usage.length; i++) {
            String s = alias == null ? usage[i] : usage[i].replace("<command>", alias);
            MiscUtil.errorMessage(sender, (i == 0 ? prefix : indent) + s);
        }
    }

    protected void showUsage(CommandSender sender, String alias) {
        showUsage(sender, alias, "Usage: ");
    }

    protected void showUsage(CommandSender sender) {
        showUsage(sender, getMatchedCommand().getCommand());
    }

    CommandRecord getMatchedCommand() {
        return matchedCommand;
    }

    protected String[] getMatchedArgs() {
        return matchedArgs;
    }

    protected void setPermissionNode(String node) {
        this.permissionNode = node;
    }

    protected void setUsage(String usage) {
        this.usage = new String[]{usage};
    }

    protected void setUsage(String[] usage) {
        this.usage = usage;
    }

    protected String[] getUsage() {
        return usage;
    }

    protected void setOptions(String... optSpec) {
        for (String opt : optSpec) {
            String[] parts = opt.split(":");
            if (parts.length == 1) {
                options.put(parts[0], OptType.BOOL);
            } else if (parts[1].startsWith("i")) {
                options.put(parts[0], OptType.INT);
            } else if (parts[1].startsWith("d")) {
                options.put(parts[0], OptType.DOUBLE);
            } else if (parts[1].startsWith("s")) {
                options.put(parts[0], OptType.STRING);
            }
        }
    }

    protected String getPermissionNode() {
        return permissionNode;
    }

    public boolean isQuotedArgs() {
        return quotedArgs;
    }

    protected boolean hasOption(String opt) {
        return optVals.containsKey(opt);
    }

    protected Object getOption(String opt) {
        return optVals.get(opt);
    }

    protected int getIntOption(String opt) {
        return getIntOption(opt, 0);
    }

    protected int getIntOption(String opt, int def) {
        if (!optVals.containsKey(opt)) {
            return def;
        }
        return (Integer) optVals.get(opt);
    }

    protected String getStringOption(String opt) {
        return getStringOption(opt, null);
    }

    protected String getStringOption(String opt, String def) {
        if (!optVals.containsKey(opt)) {
            return def;
        }
        return (String) optVals.get(opt);
    }

    protected double getDoubleOption(String opt) {
        return getDoubleOption(opt, 0.0);
    }

    protected double getDoubleOption(String opt, double def) {
        if (!optVals.containsKey(opt)) {
            return def;
        }
        return (Double) optVals.get(opt);
    }

    protected boolean getBooleanOption(String opt) {
        return getBooleanOption(opt, false);
    }

    protected boolean getBooleanOption(String opt, boolean def) {
        if (!optVals.containsKey(opt)) {
            return def;
        }
        return (Boolean) optVals.get(opt);
    }

    public void setQuotedArgs(boolean usesQuotedArgs) {
        this.quotedArgs = usesQuotedArgs;
    }

    protected void notFromConsole(CommandSender sender) throws DHUtilsException {
        if (!(sender instanceof Player)) {
            throw new DHUtilsException("This command can't be run from the console.");
        }
    }

    protected String combine(String[] args, int idx) {
        return combine(args, idx, args.length - 1);
    }

    protected String combine(String[] args, int idx1, int idx2) {
        StringBuilder result = new StringBuilder();
        for (int i = idx1; i <= idx2 && i < args.length; i++) {
            result.append(args[i]);
            if (i < idx2) {
                result.append(" ");
            }
        }
        return result.toString();
    }

    @Deprecated
    protected Map<String, String> parseCommandOptions(String[] args, int start) {
        Map<String, String> res = new HashMap<String, String>();

        Pattern pattern = Pattern.compile("^-(.+)"); //$NON-NLS-1$

        for (int i = start; i < args.length; ++i) {
            Matcher matcher = pattern.matcher(args[i]);
            if (matcher.find()) {
                String opt = matcher.group(1);
                try {
                    res.put(opt, args[++i]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    res.put(opt, null);
                }
            }
        }
        return res;
    }

    /**
     * Return a list of possible completions for the command for the given
     * arguments. Override this in subclasses.
     *
     * @param sender the player doing the tab completion
     * @param args   the argument list
     * @return a list of possible completions
     */
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        return noCompletions(sender);
    }

    /**
     * Return an empty list of possible completions.
     *
     * @return an empty string list
     */
    protected List<String> noCompletions() {
        return CommandManager.noCompletions();
    }

    /**
     * Return an empty list of possible completions, also playing an alert to
     * the sender if they are a player.
     *
     * @param sender the command sender trying to get a completion
     * @return an empty string list
     */
    protected List<String> noCompletions(CommandSender sender) {
        return CommandManager.noCompletions(sender);
    }

    /**
     * Given a collection of String and a String prefix, return a List of those
     * collection members which start with the prefix. The sender is used for
     * notification purposes if no members are matched, and may be null.
     *
     * @param sender
     * @param c
     * @param prefix
     * @return
     */
    protected List<String> filterPrefix(CommandSender sender, Collection<String> c,
                                        String prefix) {
        List<String> res = new ArrayList<String>();
        for (String s : c) {
            if (prefix == null || prefix.isEmpty()
                    || s.toLowerCase().startsWith(prefix.toLowerCase())) {
                res.add(s);
            }
        }
        return getResult(res, sender, true);
    }

    /**
     * Given a list of Strings, return the list, possibly sorted. If a sender is
     * supplied, notify the sender if the list is empty.
     *
     * @param res
     * @param sender
     * @param sorted
     * @return
     */
    protected List<String> getResult(List<String> res, CommandSender sender,
                                     boolean sorted) {
        if (res.isEmpty()) {
            return sender == null ? noCompletions() : noCompletions(sender);
        }
        return sorted ? MiscUtil.asSortedList(res) : res;
    }

    protected List<String> getEnumCompletions(CommandSender sender,
                                              Class<? extends Enum<?>> c, String prefix) {
        List<String> res = new ArrayList<String>();
        for (Object o1 : c.getEnumConstants()) {
            res.add(o1.toString());
        }
        return filterPrefix(sender, res, prefix);
    }

    protected List<String> getConfigCompletions(CommandSender sender,
                                                ConfigurationSection config, String prefix) {
        List<String> res = new ArrayList<String>();
        for (String k : config.getKeys(true)) {
            if (!config.isConfigurationSection(k)) {
                res.add(k);
            }
        }
        return filterPrefix(sender, res, prefix);
    }

    protected List<String> getConfigValueCompletions(CommandSender sender, String key,
                                                     Object obj, String desc, String prefix) {
        List<String> res = new ArrayList<String>();
        if (obj instanceof Enum<?>) {
            MiscUtil.alertMessage(sender, key + ":" + desc);
            for (Object o1 : obj.getClass().getEnumConstants()) {
                res.add(o1.toString());
            }
        } else if (obj instanceof Boolean) {
            MiscUtil.alertMessage(sender, key + ":" + desc);
            res.add("true");
            res.add("false");
        } else {
            MiscUtil.alertMessage(sender, key + " = <" + obj.getClass().getSimpleName()
                    + ">" + desc);
        }
        return filterPrefix(sender, res, prefix);
    }

    /**
     * Represents a single command record: command plus subcommands. A command
     * object contains one or more of these records.
     */
    class CommandRecord {
        private final String command;
        private final String[] subCommands;

        public CommandRecord(String[] fields) {
            this.command = fields[0];
            this.subCommands = new String[fields.length - 1];
            for (int i = 1; i < fields.length; i++) {
                subCommands[i - 1] = fields[i];
            }
        }

        @Override
        public String toString() {
            return command + " " + Joiner.on(" ").join(subCommands);
        }

        public int size() {
            return subCommands.length;
        }

        public String getCommand() {
            return command;
        }

        public String getSubCommand(int idx) {
            return subCommands[idx];
        }

        public String getLastSubCommand() {
            return subCommands[subCommands.length - 1];
        }
    }

    @Override
    public int compareTo(AbstractCommand other) {
        List<CommandRecord> recs = getCmdRecs();
        List<CommandRecord> recs2 = other.getCmdRecs();
        return recs.toString().compareTo(recs2.toString());
    }

    @Override
    public String toString() {
        return "[" + Joiner.on(",").join(cmdRecs) + "]";
    }
}
