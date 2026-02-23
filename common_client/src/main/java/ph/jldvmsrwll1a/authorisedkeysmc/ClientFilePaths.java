package ph.jldvmsrwll1a.authorisedkeysmc;

import static ph.jldvmsrwll1a.authorisedkeysmc.Constants.MOD_DIR_NAME;

import java.nio.file.Path;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

public class ClientFilePaths {
    public final Path MOD_DIR;
    public final Path CONFIG_DIR;

    public final Path CONFIG_PATH;
    public final Path KEY_PAIRS_DIR;
    public final Path KNOWN_HOSTS_PATH;
    public final Path KEY_USES_PATH;

    public ClientFilePaths(IPlatformHelper platform) {
        MOD_DIR = platform.getGameDirectory().resolve(MOD_DIR_NAME);
        CONFIG_DIR = platform.getConfigDirectory().resolve(MOD_DIR_NAME);

        CONFIG_PATH = CONFIG_DIR.resolve("client.properties");

        KEY_PAIRS_DIR = MOD_DIR.resolve("secrets");
        KNOWN_HOSTS_PATH = MOD_DIR.resolve("known_hosts.json");
        KEY_USES_PATH = MOD_DIR.resolve("key_uses.json");
    }
}
