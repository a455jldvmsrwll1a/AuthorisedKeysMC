package ph.jldvmsrwll1a.authorisedkeysmc;

import ph.jldvmsrwll1a.authorisedkeysmc.client.ClientKeyPairs;

public class AuthorisedKeysModClient {
    public static ClientKeyPairs KEY_PAIRS;

    public static void init() {
        KEY_PAIRS = new ClientKeyPairs();
    }
}
