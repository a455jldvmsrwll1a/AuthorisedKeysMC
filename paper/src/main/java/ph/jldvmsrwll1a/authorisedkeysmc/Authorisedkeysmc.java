package ph.jldvmsrwll1a.authorisedkeysmc;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ph.jldvmsrwll1a.authorisedkeysmc.command.ModCommands;
import ph.jldvmsrwll1a.authorisedkeysmc.net.NetworkProcessor;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.PaperPlatformHelper;

public final class Authorisedkeysmc extends JavaPlugin implements Listener {
    public static final Key PLUGIN_KEY = Key.key(Constants.MOD_ID, "net_handler");

    public static final NetworkProcessor CHANNEL_INIT = new NetworkProcessor();

    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::registerCommands);
        getServer().getPluginManager().registerEvents(new Events(this), this);
        ChannelInitializeListenerHolder.addListener(PLUGIN_KEY, CHANNEL_INIT::inject);

        AkmcCore.init(new PaperPlatformHelper(this));
    }

    @Override
    public void onDisable() {
        ChannelInitializeListenerHolder.removeListener(PLUGIN_KEY);
    }

    private void registerCommands(ReloadableRegistrarEvent<Commands> commandsReloadableRegistrarEvent) {
        ModCommands.register(commandsReloadableRegistrarEvent.registrar());
    }
}
