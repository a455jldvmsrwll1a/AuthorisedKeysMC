package ph.jldvmsrwll1a.authorisedkeysmc;

import static ph.jldvmsrwll1a.authorisedkeysmc.Constants.MOD_DIR_NAME;

import java.nio.file.Path;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

public class FilePaths {
    public final Path MOD_DIR;
    public final Path CONFIG_DIR;

    public final Path CONFIG_PATH;

    public final Path USERS_JSON_PATH;
    public final Path SERVER_SECRET_PATH;

    public FilePaths(IPlatformHelper platform) {
        MOD_DIR = platform.getGameDirectory().resolve(MOD_DIR_NAME);
        CONFIG_DIR = platform.getConfigDirectory().resolve(MOD_DIR_NAME);

        CONFIG_PATH = CONFIG_DIR.resolve("server.properties");

        USERS_JSON_PATH = MOD_DIR.resolve("users.json");
        SERVER_SECRET_PATH = MOD_DIR.resolve("server_secret%s".formatted(Constants.KEY_PAIR_EXTENSION));
    }
}
