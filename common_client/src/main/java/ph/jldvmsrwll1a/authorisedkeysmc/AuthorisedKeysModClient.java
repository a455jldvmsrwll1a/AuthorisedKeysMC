package ph.jldvmsrwll1a.authorisedkeysmc;

import ph.jldvmsrwll1a.authorisedkeysmc.crypto.ClientKeyPairs;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.PrivateKeyCache;

public class AuthorisedKeysModClient {
    public static ClientKeyPairs KEY_PAIRS;
    public static KnownHosts KNOWN_HOSTS;
    public static PrivateKeyCache CACHED_KEYS;

    public static void init() {
        KEY_PAIRS = new ClientKeyPairs();
        KNOWN_HOSTS = new KnownHosts();
        CACHED_KEYS = new PrivateKeyCache();
    }
}
