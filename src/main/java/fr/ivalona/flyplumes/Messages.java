package fr.ivalona.flyplumes;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Messages {

    private final FlyPlugin plugin;
    private FileConfiguration cfg;
    private String prefix = "";

    public Messages(FlyPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        prefix = cfg.getString("prefix", "");
    }

    /** Remplace %prefix% puis les paires cle/valeur (kv = "%cle%", "valeur", ...). */
    private String apply(String text, String... kv) {
        String out = text.replace("%prefix%", prefix);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out = out.replace(kv[i], kv[i + 1]);
        }
        return out;
    }

    public Component get(String key, String... kv) {
        String raw = cfg.getString(key);
        if (raw == null) {
            return Text.parse("&cMessage manquant : " + key);
        }
        return Text.parse(apply(raw, kv));
    }

    public List<Component> getList(String key, String... kv) {
        List<Component> out = new ArrayList<>();
        for (String line : cfg.getStringList(key)) {
            out.add(Text.parse(apply(line, kv)));
        }
        return out;
    }

    public void send(CommandSender to, String key, String... kv) {
        if (cfg.isList(key)) {
            for (Component line : getList(key, kv)) {
                to.sendMessage(line);
            }
            return;
        }
        String raw = cfg.getString(key);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        to.sendMessage(get(key, kv));
    }
}
