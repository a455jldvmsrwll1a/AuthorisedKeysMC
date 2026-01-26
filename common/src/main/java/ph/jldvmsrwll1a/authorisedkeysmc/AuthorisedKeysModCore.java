package ph.jldvmsrwll1a.authorisedkeysmc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

import java.security.Security;

public class AuthorisedKeysModCore {
    public static IPlatformHelper PLATFORM;
    public static FilePaths FILE_PATHS;
    public static ServerKeypair SERVER_KEYPAIR;
    public static UserKeys USER_KEYS;

    public static void init(@NotNull IPlatformHelper platform) {
        PLATFORM = platform;
        FILE_PATHS = new FilePaths(platform);
        SERVER_KEYPAIR = new ServerKeypair();
        USER_KEYS = new UserKeys();

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
}
