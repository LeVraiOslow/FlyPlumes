package fr.ivalona.flyplumes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fabrication et reconnaissance des plumes de fly. */
public final class FeatherItems {

    private final FlyPlugin plugin;
    private final NamespacedKey idKey;
    private final Map<String, Long> seconds = new LinkedHashMap<>();
    private boolean nexoWarned;

    public FeatherItems(FlyPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "feather_id");
    }

    public void load() {
        seconds.clear();
        ConfigurationSection items = plugin.getConfig().getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String id : items.getKeys(false)) {
            long s = items.getLong(id + ".seconds", 0);
            if (s <= 0) {
                plugin.getLogger().warning("Plume '" + id + "' ignoree : 'seconds' doit etre superieur a 0.");
                continue;
            }
            seconds.put(id, s);
        }
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(seconds.keySet());
    }

    public boolean exists(String id) {
        return seconds.containsKey(id);
    }

    public long secondsOf(String id) {
        return seconds.getOrDefault(id, 0L);
    }

    /** Retourne l'id de la plume si l'item en est une, sinon null. */
    public String identify(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        return (id != null && seconds.containsKey(id)) ? id : null;
    }

    public ItemStack create(String id, int amount) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("items." + id);
        if (sec == null || !seconds.containsKey(id)) {
            return null;
        }

        ItemStack stack = null;
        String nexoId = sec.getString("nexo-id", "");
        if (!nexoId.isEmpty() && Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            stack = nexoItem(nexoId);
        }
        if (stack == null) {
            Material mat = Material.matchMaterial(sec.getString("material", "FEATHER"));
            if (mat == null || !mat.isItem() || mat.isAir()) {
                mat = Material.FEATHER;
            }
            stack = new ItemStack(mat);
        }
        stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize())));

        ItemMeta meta = stack.getItemMeta();
        String duration = plugin.timeFormat().format(seconds.get(id));

        String name = sec.getString("name", "");
        if (!name.isEmpty()) {
            meta.displayName(Text.item(name.replace("%duration%", duration)));
        }
        List<String> lore = sec.getStringList("lore");
        if (!lore.isEmpty()) {
            List<String> replaced = new ArrayList<>();
            for (String line : lore) {
                replaced.add(line.replace("%duration%", duration));
            }
            meta.lore(Text.itemLines(replaced));
        }

        String model = sec.getString("item-model", "");
        if (!model.isEmpty()) {
            NamespacedKey key = NamespacedKey.fromString(model);
            if (key != null) {
                meta.setItemModel(key);
            } else {
                plugin.getLogger().warning("item-model invalide pour '" + id + "' : " + model);
            }
        }
        if (sec.getBoolean("glow", false)) {
            meta.setEnchantmentGlintOverride(true);
        }

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Construit un item Nexo par reflexion (pas de dependance a la compilation). */
    private ItemStack nexoItem(String nexoId) {
        try {
            Class<?> api = Class.forName("com.nexomc.nexo.api.NexoItems");
            Method from = api.getMethod("itemFromId", String.class);
            Object target = null;
            if (!Modifier.isStatic(from.getModifiers())) {
                target = api.getField("INSTANCE").get(null);
            }
            Object builder = from.invoke(target, nexoId);
            if (builder == null) {
                plugin.getLogger().warning("Item Nexo introuvable : " + nexoId);
                return null;
            }
            Object built = builder.getClass().getMethod("build").invoke(builder);
            return built instanceof ItemStack is ? is : null;
        } catch (Throwable t) {
            if (!nexoWarned) {
                nexoWarned = true;
                plugin.getLogger().warning("Impossible de lire l'item Nexo '" + nexoId + "' : " + t);
            }
            return null;
        }
    }
}
