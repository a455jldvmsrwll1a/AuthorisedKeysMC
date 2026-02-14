package ph.jldvmsrwll1a.authorisedkeysmc;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

public class AuthorisedKeysModCore {
    public static IPlatformHelper PLATFORM;
    public static FilePaths FILE_PATHS;
    public static AkKeyPair SERVER_KEYPAIR;
    public static UserKeys USER_KEYS;

    public static void init(@NotNull IPlatformHelper platform) {
        PLATFORM = platform;
        FILE_PATHS = new FilePaths(platform);
        USER_KEYS = new UserKeys();

        initialiseServerKeyPair();

        Constants.LOG.info("MOD_DIR: {}", FILE_PATHS.MOD_DIR);
        Constants.LOG.info("CONFIG_DIR: {}", FILE_PATHS.CONFIG_DIR);
        Constants.LOG.info("CLIENT_CONFIG_PATH: {}", FILE_PATHS.CLIENT_CONFIG_PATH);
        Constants.LOG.info("SERVER_CONFIG_PATH: {}", FILE_PATHS.SERVER_CONFIG_PATH);
        Constants.LOG.info("KEY_PAIRS_DIR: {}", FILE_PATHS.KEY_PAIRS_DIR);
        Constants.LOG.info("AUTHORISED_KEYS_PATH: {}", FILE_PATHS.AUTHORISED_KEYS_PATH);
        Constants.LOG.info("BYPASS_LIST_PATH: {}", FILE_PATHS.BYPASS_LIST_PATH);
        Constants.LOG.info("UUID_REMAPS_PATH: {}", FILE_PATHS.UUID_REMAPS_PATH);
        Constants.LOG.info("HISTORY_PATH: {}", FILE_PATHS.HISTORY_PATH);
        Constants.LOG.info("SERVER_SECRET_PATH: {}", FILE_PATHS.SERVER_SECRET_PATH);

        Security.addProvider(new BouncyCastleProvider());
    }

    private static void initialiseServerKeyPair() {
        try {
            SERVER_KEYPAIR = AkKeyPair.fromFile(AuthorisedKeysModCore.FILE_PATHS.SERVER_SECRET_PATH, "");

            if (SERVER_KEYPAIR.requiresDecryption()) {
                throw new RuntimeException("AKMC: the server key pair's private key must NOT be encrypted!");
            }

            Constants.LOG.info("Loaded existing server key pair.");
        } catch (IOException ignored) {
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
