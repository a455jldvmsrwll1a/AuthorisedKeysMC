package ph.jldvmsrwll1a.authorisedkeysmc;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.jetbrains.annotations.NotNull;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

public final class AuthorisedKeysModCore {
    public static IPlatformHelper PLATFORM;
    public static FilePaths FILE_PATHS;
    public static AkKeyPair SERVER_KEYPAIR;
    public static UserKeys USER_KEYS;
    public static ServerConfig CONFIG;

    private AuthorisedKeysModCore() {}

    public static void init(@NotNull IPlatformHelper platform) {
        PLATFORM = platform;
        FILE_PATHS = new FilePaths(platform);
        CONFIG = new ServerConfig();
        USER_KEYS = new UserKeys();

        reload();
        initialiseServerKeyPair();
    }

    public static synchronized void reload() {
        USER_KEYS.read();
        CONFIG = ServerConfig.fromDisk();

        Constants.LOG.info("AKMC: loaded server files!");
    }

    private static void initialiseServerKeyPair() {
        try {
            SERVER_KEYPAIR = AkKeyPair.fromFile(AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_PATH, "");

            if (SERVER_KEYPAIR.requiresDecryption()) {
                throw new RuntimeException("AKMC: the server key pair's private key must NOT be encrypted!");
            }

            Constants.LOG.info("Loaded existing server key pair.");
        } catch (Exception ignored) {
            Constants.LOG.warn("Could not find existing server key pair!");

            try {
                SERVER_KEYPAIR = AkKeyPair.generate(SecureRandom.getInstanceStrong(), "");
                SERVER_KEYPAIR.writeFile(AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_PATH);

                Constants.LOG.info("Generated new server key pair.");
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
