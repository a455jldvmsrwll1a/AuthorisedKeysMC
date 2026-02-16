package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.PortalScreen;

public final class AuthorisedKeysMCClient {
    public AuthorisedKeysMCClient(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new PortalScreen(parent)));

        AkmcClient.init();
    }
}
