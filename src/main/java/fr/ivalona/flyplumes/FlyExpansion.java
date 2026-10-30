package fr.ivalona.flyplumes;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Placeholders :
 *   %flyplume_time%      temps restant formate (ex: "1h 5min 3s") ou "infini"
 *   %flyplume_seconds%   temps restant en secondes, ou "infini"
 *   %flyplume_minutes%   temps restant en minutes (arrondi au-dessus), ou "infini"
 *   %flyplume_unlimited% true / false
 *   %flyplume_active%    true / false (le fly est actuellement active)
 */
public final class FlyExpansion extends PlaceholderExpansion {

    private final FlyPlugin plugin;

    public FlyExpansion(FlyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "flyplume";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Ivalona";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Player online = player.getPlayer();
        boolean unlimited = online != null && plugin.fly().isUnlimited(online);
        long seconds = plugin.fly().getSeconds(player.getUniqueId());
        String infinite = plugin.timeFormat().infinite();

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "time" -> unlimited ? infinite : plugin.timeFormat().format(seconds);
            case "seconds" -> unlimited ? infinite : String.valueOf(seconds);
            case "minutes" -> unlimited ? infinite : String.valueOf((seconds + 59) / 60);
            case "unlimited" -> String.valueOf(unlimited);
            case "active" -> String.valueOf(plugin.fly().isActive(player.getUniqueId()));
            default -> null;
        };
    }
}
