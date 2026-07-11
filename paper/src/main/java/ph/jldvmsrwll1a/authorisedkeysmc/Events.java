package ph.jldvmsrwll1a.authorisedkeysmc;

import com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;

public class Events implements Listener {
    private final Plugin plugin;

    public Events(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        Constants.LOG.info("hello, {}", event.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void handleWhitelist(ProfileWhitelistVerifyEvent event) {
        if (!event.isWhitelistEnabled()) {
            return;
        }

        if (!AkmcCore.CONFIG.matchPlayerListByName) {
            return;
        }

        event.setWhitelisted(plugin.getServer().getWhitelistedPlayers().stream()
                .anyMatch(
                        offlinePlayer -> Objects.equals(event.getPlayerProfile().getName(), offlinePlayer.getName())));
    }
}
