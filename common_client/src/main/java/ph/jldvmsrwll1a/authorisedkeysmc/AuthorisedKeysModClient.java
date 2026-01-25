package ph.jldvmsrwll1a.authorisedkeysmc;

public class AuthorisedKeysModClient {
    public static ClientKeyPairs KEY_PAIRS;
    public static KnownHosts KNOWN_HOSTS;

    public static void init() {
        KEY_PAIRS = new ClientKeyPairs();
        KNOWN_HOSTS = new KnownHosts();
    }
}
