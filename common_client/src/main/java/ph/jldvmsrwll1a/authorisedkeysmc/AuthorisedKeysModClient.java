package ph.jldvmsrwll1a.authorisedkeysmc;

public class AuthorisedKeysModClient {
    public static ClientKeyPairs KEY_PAIRS;
    public static KnownServers KNOWN_SERVERS;

    public static void init() {
        KEY_PAIRS = new ClientKeyPairs();
        KNOWN_SERVERS = new KnownServers();
    }
}
