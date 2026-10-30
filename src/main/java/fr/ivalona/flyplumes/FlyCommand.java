package fr.ivalona.flyplumes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /fly : active ou desactive le fly. */
public final class FlyCommand implements CommandExecutor {

    private final FlyPlugin plugin;

    public FlyCommand(FlyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Commande reservee aux joueurs.");
            return true;
        }
        if (!p.hasPermission("flyplumes.use")) {
            plugin.messages().send(p, "no-permission");
            return true;
        }

        FlyManager fly = plugin.fly();
        UUID id = p.getUniqueId();

        if (fly.isActive(id)) {
            fly.disable(p);
            plugin.messages().send(p, "fly-disabled");
            return true;
        }

        switch (fly.enable(p)) {
            case ENABLED -> plugin.messages().send(p, "fly-enabled",
                    "%time%", plugin.timeFormat().format(fly.getSeconds(id)));
            case ENABLED_UNLIMITED -> plugin.messages().send(p, "fly-enabled-unlimited");
            case NO_TIME -> plugin.messages().send(p, "no-time");
            case NOT_NEEDED -> plugin.messages().send(p, "creative-no-need");
            case WORLD_DISABLED -> plugin.messages().send(p, "world-disabled");
        }
        return true;
    }
}
