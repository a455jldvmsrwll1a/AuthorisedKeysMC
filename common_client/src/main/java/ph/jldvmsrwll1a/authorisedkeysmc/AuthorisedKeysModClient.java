package ph.jldvmsrwll1a.authorisedkeysmc;

import ph.jldvmsrwll1a.authorisedkeysmc.crypto.ClientKeyPairs;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.PrivateKeyCache;
import ph.jldvmsrwll1a.authorisedkeysmc.servers.KeyUses;
import ph.jldvmsrwll1a.authorisedkeysmc.servers.KnownHosts;

public class AuthorisedKeysModClient {
    public static ClientKeyPairs KEY_PAIRS;
    public static KnownHosts KNOWN_HOSTS;
    public static KeyUses KEY_USES;
    public static PrivateKeyCache CACHED_KEYS;

    public static void init() {
        KEY_PAIRS = new ClientKeyPairs();
        KNOWN_HOSTS = new KnownHosts();
        KEY_USES = new KeyUses();
        CACHED_KEYS = new PrivateKeyCache();
    }
}
