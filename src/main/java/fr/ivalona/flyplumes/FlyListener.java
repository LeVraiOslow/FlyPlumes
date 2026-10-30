package fr.ivalona.flyplumes;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class FlyListener implements Listener {

    private final FlyPlugin plugin;

    public FlyListener(FlyPlugin plugin) {
        this.plugin = plugin;
    }

    /** Clic droit avec une plume : ajoute du temps de fly. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        String id = plugin.items().identify(hand);
        if (id == null) {
            return;
        }
        event.setCancelled(true);

        if (!p.hasPermission("flyplumes.use")) {
            plugin.messages().send(p, "no-permission");
            return;
        }
        FlyManager fly = plugin.fly();
        if (fly.isUnlimited(p)) {
            plugin.messages().send(p, "item-unlimited");
            return;
        }

        long add = plugin.items().secondsOf(id);
        long max = plugin.getConfig().getLong("fly.max-seconds", 0);
        long current = fly.getSeconds(p.getUniqueId());
        if (max > 0 && current + add > max) {
            plugin.messages().send(p, "item-max-reached", "%max%", plugin.timeFormat().format(max));
            return;
        }

        if (hand.getAmount() <= 1) {
            p.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            p.getInventory().setItemInMainHand(hand);
        }

        long total = fly.addSeconds(p.getUniqueId(), add);
        plugin.messages().send(p, "item-redeemed",
                "%added%", plugin.timeFormat().format(add),
                "%total%", plugin.timeFormat().format(total));
    }

    /** Pas de degats de chute apres la fin du fly. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (event.getEntity() instanceof Player p && plugin.fly().absorbFall(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (plugin.fly().isActive(p.getUniqueId())) {
            // Evite qu'il se reconnecte avec le fly reste active dans ses donnees.
            plugin.fly().disable(p);
        }
        plugin.fly().forget(p.getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        FlyManager fly = plugin.fly();
        if (fly.isActive(p.getUniqueId()) && fly.isWorldDisabled(p)) {
            fly.disable(p);
            plugin.messages().send(p, "world-disabled");
        }
    }
}
