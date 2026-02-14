package ph.jldvmsrwll1a.authorisedkeysmc;

import static ph.jldvmsrwll1a.authorisedkeysmc.Constants.MOD_DIR_NAME;

import java.nio.file.Path;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

public class FilePaths {
    public final Path MOD_DIR;
    public final Path CONFIG_DIR;

    public final Path CLIENT_CONFIG_PATH;
    public final Path SERVER_CONFIG_PATH;

    public final Path KEY_PAIRS_DIR;
    public final Path KNOWN_HOSTS_PATH;
    public final Path KEY_USES_PATH;

    public final Path AUTHORISED_KEYS_PATH;
    public final Path BYPASS_LIST_PATH;
    public final Path UUID_REMAPS_PATH;
    public final Path HISTORY_PATH;
    public final Path SERVER_SECRET_PATH;

    public FilePaths(IPlatformHelper platform) {
        MOD_DIR = platform.getGameDirectory().resolve(MOD_DIR_NAME);
        CONFIG_DIR = platform.getConfigDirectory().resolve(MOD_DIR_NAME);

        CLIENT_CONFIG_PATH = CONFIG_DIR.resolve("client.toml");
        SERVER_CONFIG_PATH = CONFIG_DIR.resolve("server.toml");

        KEY_PAIRS_DIR = MOD_DIR.resolve("secrets");
        KNOWN_HOSTS_PATH = MOD_DIR.resolve("known_hosts.json");
        KEY_USES_PATH = MOD_DIR.resolve("key_uses.json");

        AUTHORISED_KEYS_PATH = MOD_DIR.resolve("authorised_keys.json");
        BYPASS_LIST_PATH = MOD_DIR.resolve("bypass.json");
        UUID_REMAPS_PATH = MOD_DIR.resolve("uuid_remaps.json");
        HISTORY_PATH = MOD_DIR.resolve("history.json");
        SERVER_SECRET_PATH = MOD_DIR.resolve("server_secret%s".formatted(Constants.KEY_PAIR_EXTENSION));
    }
}
