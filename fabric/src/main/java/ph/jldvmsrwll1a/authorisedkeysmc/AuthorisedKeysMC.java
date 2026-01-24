package ph.jldvmsrwll1a.authorisedkeysmc;

import net.fabricmc.api.ModInitializer;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.FabricPlatformHelper;

public class AuthorisedKeysMC implements ModInitializer {

    @Override
    public void onInitialize() {
        AuthorisedKeysModCore.init(new FabricPlatformHelper());
    }
}
