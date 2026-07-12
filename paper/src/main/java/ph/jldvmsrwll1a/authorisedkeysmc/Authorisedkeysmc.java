package ph.jldvmsrwll1a.authorisedkeysmc;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ph.jldvmsrwll1a.authorisedkeysmc.command.ModCommands;
import ph.jldvmsrwll1a.authorisedkeysmc.net.NetworkProcessor;
import ph.jldvmsrwll1a.authorisedkeysmc.net.PacketInterceptor;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.PaperPlatformHelper;

public final class Authorisedkeysmc extends JavaPlugin implements Listener {
    public static final Key PLUGIN_KEY = Key.key(Constants.MOD_ID, "net_handler");

    public static final NetworkProcessor CHANNEL_INIT = new NetworkProcessor();

    public static final ConcurrentHashMap<ServerLoginPacketListenerImpl, PacketInterceptor> PENDING_LOGINS =
            new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::registerCommands);
        ChannelInitializeListenerHolder.addListener(PLUGIN_KEY, CHANNEL_INIT::inject);
        getServer().getPluginManager().registerEvents(new Events(this), this);

        AkmcCore.init(new PaperPlatformHelper(this));

        new BukkitRunnable() {

            @Override
            public void run() {
                boolean cleaned = PENDING_LOGINS.entrySet().removeIf(entry -> !entry.getValue().isConnected());

                if (cleaned) {
                    Constants.LOG.warn("Leftover pending login was cleaned up.");
                }
            }
        }.runTaskTimerAsynchronously(this, 20, 20);
    }

    @Override
    public void onDisable() {
        ChannelInitializeListenerHolder.removeListener(PLUGIN_KEY);
    }

    private void registerCommands(ReloadableRegistrarEvent<Commands> commandsReloadableRegistrarEvent) {
        ModCommands.register(commandsReloadableRegistrarEvent.registrar());
    }
}
