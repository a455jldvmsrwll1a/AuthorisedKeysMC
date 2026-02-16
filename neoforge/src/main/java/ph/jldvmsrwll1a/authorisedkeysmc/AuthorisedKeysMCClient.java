package ph.jldvmsrwll1a.authorisedkeysmc;

import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.PortalScreen;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class AuthorisedKeysMCClient {

    public AuthorisedKeysMCClient(ModContainer container) {
        NeoForge.EVENT_BUS.addListener(AuthorisedKeysMCClient::onTick);
        container.registerExtensionPoint(IConfigScreenFactory.class, (Supplier<IConfigScreenFactory>)
                () -> (minecraft, parent) -> new PortalScreen(parent));

        AkmcClient.init();
    }

    public static void onTick(ClientTickEvent.Post event) {
        AkmcClient.tick();
    }
}
