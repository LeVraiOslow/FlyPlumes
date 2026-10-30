package fr.ivalona.flyplumes;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /flyplume : administration. */
public final class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS =
            List.of("give", "addtime", "removetime", "settime", "check", "reload");

    private final FlyPlugin plugin;

    public AdminCommand(FlyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages msg = plugin.messages();
        if (!sender.hasPermission("flyplumes.admin")) {
            msg.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            msg.send(sender, "admin-usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadAll();
                msg.send(sender, "reloaded");
            }
            case "give" -> give(sender, args);
            case "addtime", "removetime", "settime" -> changeTime(sender, sub, args);
            case "check" -> check(sender, args);
            default -> msg.send(sender, "admin-usage");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        Messages msg = plugin.messages();
        if (args.length < 3) {
            msg.send(sender, "admin-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg.send(sender, "player-offline", "%player%", args[1]);
            return;
        }
        String id = args[2];
        if (!plugin.items().exists(id)) {
            msg.send(sender, "invalid-item", "%id%", id);
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                amount = -1;
            }
            if (amount < 1 || amount > 2304) {
                msg.send(sender, "invalid-amount");
                return;
            }
        }

        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = plugin.items().create(id, remaining);
            if (stack == null) {
                msg.send(sender, "invalid-item", "%id%", id);
                return;
            }
            remaining -= Math.max(1, stack.getAmount());
            Map<Integer, ItemStack> left = target.getInventory().addItem(stack);
            for (ItemStack extra : left.values()) {
                target.getWorld().dropItem(target.getLocation(), extra);
            }
        }

        msg.send(sender, "item-given", "%amount%", String.valueOf(amount), "%id%", id,
                "%player%", target.getName());
        msg.send(target, "item-received", "%amount%", String.valueOf(amount));
    }

    private void changeTime(CommandSender sender, String sub, String[] args) {
        Messages msg = plugin.messages();
        if (args.length < 3) {
            msg.send(sender, "admin-usage");
            return;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            msg.send(sender, "player-not-found", "%player%", args[1]);
            return;
        }
        long seconds = TimeFormat.parse(args[2]);
        if (seconds < 0) {
            msg.send(sender, "invalid-duration");
            return;
        }

        FlyManager fly = plugin.fly();
        String name = target.getName() != null ? target.getName() : args[1];
        String time = plugin.timeFormat().format(seconds);
        switch (sub) {
            case "addtime" -> {
                long total = fly.addSeconds(target.getUniqueId(), seconds);
                msg.send(sender, "time-added", "%time%", time, "%player%", name,
                        "%total%", plugin.timeFormat().format(total));
            }
            case "removetime" -> {
                long total = fly.addSeconds(target.getUniqueId(), -seconds);
                msg.send(sender, "time-removed", "%time%", time, "%player%", name,
                        "%total%", plugin.timeFormat().format(total));
            }
            default -> {
                fly.setSeconds(target.getUniqueId(), seconds);
                msg.send(sender, "time-set", "%player%", name, "%total%", time);
            }
        }
    }

    private void check(CommandSender sender, String[] args) {
        Messages msg = plugin.messages();
        if (args.length < 2) {
            msg.send(sender, "admin-usage");
            return;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            msg.send(sender, "player-not-found", "%player%", args[1]);
            return;
        }
        String name = target.getName() != null ? target.getName() : args[1];
        Player online = target.getPlayer();
        String total = (online != null && plugin.fly().isUnlimited(online))
                ? plugin.timeFormat().infinite()
                : plugin.timeFormat().format(plugin.fly().getSeconds(target.getUniqueId()));
        msg.send(sender, "time-check", "%player%", name, "%total%", total);
    }

    /** Joueur connecte, sinon joueur deja connu du serveur. */
    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("flyplumes.admin")) {
            return out;
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            addMatching(out, SUBS, last);
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(last)) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            addMatching(out, new ArrayList<>(plugin.items().ids()), last);
        } else if (args.length == 3 && args[0].toLowerCase(Locale.ROOT).endsWith("time")) {
            addMatching(out, List.of("10m", "30m", "1h"), last);
        }
        return out;
    }

    private static void addMatching(List<String> out, List<String> options, String prefix) {
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(option);
            }
        }
    }
}
