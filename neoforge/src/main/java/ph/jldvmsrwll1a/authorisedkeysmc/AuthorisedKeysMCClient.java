package ph.jldvmsrwll1a.authorisedkeysmc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class AuthorisedKeysMCClient {

    public AuthorisedKeysMCClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(AuthorisedKeysMCClient::onTick);

        AuthorisedKeysModClient.init();
    }

    public static void onTick(ClientTickEvent.Post event) {
        AuthorisedKeysModClient.tick();
    }
}
