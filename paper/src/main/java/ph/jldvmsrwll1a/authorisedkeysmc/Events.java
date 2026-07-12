package ph.jldvmsrwll1a.authorisedkeysmc;

import com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.connection.PaperPlayerLoginConnection;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import ph.jldvmsrwll1a.authorisedkeysmc.net.PacketInterceptor;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ServerLoginHandler;

public class Events implements Listener {

    private static final Field PPLC_LOGIN_LISTENER_IMPL;

    private final Plugin plugin;

    public Events(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        Constants.LOG.info("hello, {}", event.getName());
        Constants.LOG.info("onPlayerPreJoin: offline profile {}", event.getPlayerProfile());

        ServerLoginPacketListenerImpl login = getPLCLoginListener((PaperPlayerLoginConnection) event.getConnection());
        PacketInterceptor interceptor = Authorisedkeysmc.PENDING_LOGINS.remove(login);
        if (interceptor == null) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Internal server login error occured. (AuthorisedKeysMC)"));

            return;
        }

        GameProfile profile = new GameProfile(
                event.getPlayerProfile().getId(), event.getPlayerProfile().getName());
        ServerLoginHandler handler =
                new ServerLoginHandler(login, login.connection, profile, interceptor.getSessionHash());
        interceptor.setMailbox(handler.getSender());

        while (true) {
            if (handler.finished()) {
                event.allow();
                break;
            } else if (!interceptor.isConnected()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("??? bye"));
                break;
            }

            handler.tick();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        interceptor.stop();
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

    private static ServerLoginPacketListenerImpl getPLCLoginListener(PaperPlayerLoginConnection pplc) {
        try {
            return (ServerLoginPacketListenerImpl) PPLC_LOGIN_LISTENER_IMPL.get(pplc);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            PPLC_LOGIN_LISTENER_IMPL = PaperPlayerLoginConnection.class.getDeclaredField("packetListener");
            PPLC_LOGIN_LISTENER_IMPL.setAccessible(true);
        } catch (NoSuchFieldException | InaccessibleObjectException e) {
            throw new RuntimeException(e);
        }
    }
}
