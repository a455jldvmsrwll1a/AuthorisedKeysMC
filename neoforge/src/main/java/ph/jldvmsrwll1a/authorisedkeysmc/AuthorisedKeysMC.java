package ph.jldvmsrwll1a.authorisedkeysmc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.NeoForgePlatformHelper;

@Mod(Constants.MOD_ID)
public class AuthorisedKeysMC {

    public AuthorisedKeysMC(IEventBus eventBus) {
        AuthorisedKeysModCore.init(new NeoForgePlatformHelper());
    }
}