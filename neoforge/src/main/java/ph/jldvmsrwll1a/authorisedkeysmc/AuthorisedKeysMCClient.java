package ph.jldvmsrwll1a.authorisedkeysmc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class AuthorisedKeysMCClient {

    public AuthorisedKeysMCClient(IEventBus eventBus) {
        AuthorisedKeysModClient.init();
    }
}
