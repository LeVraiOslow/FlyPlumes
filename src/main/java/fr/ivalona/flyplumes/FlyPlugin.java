package fr.ivalona.flyplumes;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlyPlugin extends JavaPlugin {

    private Messages messages;
    private TimeFormat timeFormat;
    private FeatherItems items;
    private FlyManager fly;
    private FlyExpansion expansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        messages.load();
        timeFormat = new TimeFormat(this);
        items = new FeatherItems(this);
        items.load();
        fly = new FlyManager(this);
        fly.load();
        fly.start();

        PluginCommand flyCmd = getCommand("fly");
        if (flyCmd != null) {
            flyCmd.setExecutor(new FlyCommand(this));
        }
        PluginCommand adminCmd = getCommand("flyplume");
        if (adminCmd != null) {
            AdminCommand admin = new AdminCommand(this);
            adminCmd.setExecutor(admin);
            adminCmd.setTabCompleter(admin);
        }
        Bukkit.getPluginManager().registerEvents(new FlyListener(this), this);

        registerPlaceholders();

        getLogger().info(items.ids().size() + " plume(s) chargee(s)."
                + (Bukkit.getPluginManager().isPluginEnabled("Nexo") ? " Nexo detecte." : ""));
    }

    @Override
    public void onDisable() {
        if (fly != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (fly.isActive(p.getUniqueId())) {
                    fly.disable(p);
                }
            }
            fly.stop();
            fly.saveNow();
        }
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            expansion = new FlyExpansion(this);
            expansion.register();
        } else {
            getLogger().info("PlaceholderAPI absent : les placeholders %flyplume_*% ne seront pas disponibles.");
        }
    }

    public void reloadAll() {
        reloadConfig();
        messages.load();
        items.load();
        fly.start();
    }

    public Messages messages() {
        return messages;
    }

    public TimeFormat timeFormat() {
        return timeFormat;
    }

    public FeatherItems items() {
        return items;
    }

    public FlyManager fly() {
        return fly;
    }
}
