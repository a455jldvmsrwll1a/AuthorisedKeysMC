package ph.jldvmsrwll1a.authorisedkeysmc;

import net.fabricmc.api.ClientModInitializer;

public class AuthorisedKeysMCClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AuthorisedKeysModClient.init();
    }
}
