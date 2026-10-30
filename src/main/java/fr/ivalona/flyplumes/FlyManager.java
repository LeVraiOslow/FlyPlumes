package fr.ivalona.flyplumes;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Temps de fly des joueurs, etat du fly et protection contre les chutes. */
public final class FlyManager {

    public static final String UNLIMITED_PERMISSION = "flyplumes.unlimited";

    public enum Result { ENABLED, ENABLED_UNLIMITED, NO_TIME, NOT_NEEDED, WORLD_DISABLED }

    private final FlyPlugin plugin;
    private final File dataFile;
    private final Map<UUID, Long> times = new ConcurrentHashMap<>();
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> noFallUntil = new ConcurrentHashMap<>();
    private volatile boolean dirty;
    private BukkitTask tickTask;
    private BukkitTask saveTask;

    public FlyManager(FlyPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    // ------------------------------------------------------------ donnees

    public void load() {
        times.clear();
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(dataFile);
        var section = yml.getConfigurationSection("times");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                long value = section.getLong(key, 0);
                if (value > 0) {
                    times.put(UUID.fromString(key), value);
                }
            } catch (IllegalArgumentException ignored) {
                // cle invalide : on ignore la ligne
            }
        }
    }

    public void saveNow() {
        Map<UUID, Long> snapshot = new HashMap<>(times);
        writeSnapshot(snapshot);
        dirty = false;
    }

    private void writeSnapshot(Map<UUID, Long> snapshot) {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : snapshot.entrySet()) {
            if (e.getValue() > 0) {
                yml.set("times." + e.getKey(), e.getValue());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder data.yml : " + ex.getMessage());
        }
    }

    public void start() {
        stop();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        long interval = Math.max(10, plugin.getConfig().getLong("save-interval-seconds", 60)) * 20L;
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirty) {
                dirty = false;
                writeSnapshot(new HashMap<>(times));
            }
        }, interval, interval);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    // ------------------------------------------------------------ temps

    public long getSeconds(UUID id) {
        return times.getOrDefault(id, 0L);
    }

    public void setSeconds(UUID id, long seconds) {
        long value = Math.max(0, seconds);
        if (value == 0) {
            times.remove(id);
        } else {
            times.put(id, value);
        }
        dirty = true;
    }

    public long addSeconds(UUID id, long seconds) {
        long total = Math.max(0, getSeconds(id) + seconds);
        setSeconds(id, total);
        return total;
    }

    public boolean isUnlimited(Player player) {
        return player.hasPermission(UNLIMITED_PERMISSION);
    }

    public boolean isActive(UUID id) {
        return active.contains(id);
    }

    // ------------------------------------------------------------ fly

    private static boolean needsNoFly(Player p) {
        GameMode gm = p.getGameMode();
        return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
    }

    public boolean isWorldDisabled(Player p) {
        List<String> disabled = plugin.getConfig().getStringList("disabled-worlds");
        return disabled.contains(p.getWorld().getName());
    }

    public Result enable(Player p) {
        if (needsNoFly(p)) {
            return Result.NOT_NEEDED;
        }
        if (isWorldDisabled(p)) {
            return Result.WORLD_DISABLED;
        }
        boolean unlimited = isUnlimited(p);
        if (!unlimited && getSeconds(p.getUniqueId()) <= 0) {
            return Result.NO_TIME;
        }
        active.add(p.getUniqueId());
        noFallUntil.remove(p.getUniqueId());
        p.setAllowFlight(true);
        if (!p.isOnGround()) {
            p.setFlying(true);
        }
        return unlimited ? Result.ENABLED_UNLIMITED : Result.ENABLED;
    }

    /** Coupe le fly du joueur et lui donne une protection temporaire contre les chutes. */
    public void disable(Player p) {
        active.remove(p.getUniqueId());
        if (needsNoFly(p)) {
            return;
        }
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setFallDistance(0f);
        long seconds = Math.max(0, plugin.getConfig().getLong("fly.fall-protection-seconds", 20));
        if (seconds > 0) {
            noFallUntil.put(p.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        }
    }

    /** Appele a chaque degat de chute : true si le degat doit etre annule. */
    public boolean absorbFall(UUID id) {
        Long until = noFallUntil.get(id);
        if (until == null) {
            return false;
        }
        noFallUntil.remove(id);
        return System.currentTimeMillis() <= until;
    }

    public void forget(UUID id) {
        active.remove(id);
    }

    // ------------------------------------------------------------ boucle (1 fois par seconde)

    private void tick() {
        boolean onlyWhileFlying = plugin.getConfig().getBoolean("fly.consume-only-while-flying", true);
        Set<Integer> warnAt = new HashSet<>(plugin.getConfig().getIntegerList("fly.warn-at-seconds"));
        long now = System.currentTimeMillis();

        noFallUntil.values().removeIf(until -> until < now);

        for (UUID id : Set.copyOf(active)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                active.remove(id);
                continue;
            }
            if (needsNoFly(p) || isUnlimited(p)) {
                continue;
            }
            if (isWorldDisabled(p)) {
                disable(p);
                plugin.messages().send(p, "world-disabled");
                continue;
            }
            if (onlyWhileFlying && !p.isFlying()) {
                continue;
            }

            long left = getSeconds(id) - 1;
            if (left <= 0) {
                setSeconds(id, 0);
                disable(p);
                plugin.messages().send(p, "fly-expired");
                continue;
            }
            setSeconds(id, left);
            if (warnAt.contains((int) left)) {
                plugin.messages().send(p, "time-warning", "%time%", plugin.timeFormat().format(left));
            }
        }
    }
}
