package ph.jldvmsrwll1a.authorisedkeysmc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ph.jldvmsrwll1a.authorisedkeysmc.command.ModCommands;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.NeoForgePlatformHelper;

@Mod(value = Constants.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class AuthorisedKeysMC {
    public AuthorisedKeysMC(IEventBus eventBus) {
        AkmcCore.init(new NeoForgePlatformHelper());

        NeoForge.EVENT_BUS.addListener(this::onCommandRegistration);
    }

    private void onCommandRegistration(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }
}
