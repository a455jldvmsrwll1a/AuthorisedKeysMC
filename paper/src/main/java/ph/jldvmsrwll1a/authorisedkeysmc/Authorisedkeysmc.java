package ph.jldvmsrwll1a.authorisedkeysmc;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ph.jldvmsrwll1a.authorisedkeysmc.command.ModCommands;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.PaperPlatformHelper;

public final class Authorisedkeysmc extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Constants.LOG.info("Hello world! AuthorisedKeysMC");

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this::registerCommands);
        getServer().getPluginManager().registerEvents(new Events(this), this);

        AkmcCore.init(new PaperPlatformHelper(this));
    }

    @Override
    public void onDisable() {
        Constants.LOG.info("Bye.");
    }

    private void registerCommands(ReloadableRegistrarEvent<Commands> commandsReloadableRegistrarEvent) {
        ModCommands.register(commandsReloadableRegistrarEvent.registrar());
    }
}
