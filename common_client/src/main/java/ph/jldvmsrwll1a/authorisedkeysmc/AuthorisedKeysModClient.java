package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.ClientKeyPairs;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.PrivateKeyCache;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.FirstRunScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;
import ph.jldvmsrwll1a.authorisedkeysmc.servers.KeyUses;
import ph.jldvmsrwll1a.authorisedkeysmc.servers.KnownHosts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthorisedKeysModClient {
    public static final ExecutorService WORKER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AKMC Worker Thread");
        thread.setDaemon(true);

        return thread;
    });

    public static final PrivateKeyCache CACHED_KEYS = new PrivateKeyCache();
    public static ClientKeyPairs KEY_PAIRS;
    public static KnownHosts KNOWN_HOSTS;
    public static KeyUses KEY_USES;

    private static boolean initialised = false;
    private static boolean hasShownFirstRunScreen = false;
    private static @Nullable ClientLoginHandler currentLogin = null;

    private AuthorisedKeysModClient() {}

    public static void init() {
        KEY_PAIRS = new ClientKeyPairs();
        KNOWN_HOSTS = new KnownHosts();
        KEY_USES = new KeyUses();

        initialised = true;
    }

    public static void readFiles() {
        Validate.validState(initialised, "AKMC client core not yet initialised.");

        KNOWN_HOSTS.read();
        KEY_USES.read();
    }

    public static void setLoginHandler(ClientLoginHandler handler) {
        Minecraft.getInstance().executeBlocking(() -> {
            if (currentLogin != null && !currentLogin.disconnected()) {
                throw new IllegalStateException("Tried to set a new login handler while an old one is still active!");
            }

            currentLogin = handler;

            Constants.LOG.info("AKMC: Setting login handler.");
        });
    }

    public static void clearLoginHandler() {
        Minecraft.getInstance().executeBlocking(() -> {
            if (currentLogin == null) {
                Constants.LOG.warn("Tried to clear non-existent login handler.");

                return;
            }

            currentLogin = null;

            Constants.LOG.info("AKMC: Discarding login handler.");
        });
    }

    public static void tick() {
        Validate.validState(initialised, "AKMC client core not yet initialised.");

        if (currentLogin != null) {
            currentLogin.tick();
        }
    }

    public static boolean maybeShowFirstRunScreen(Minecraft minecraft, @Nullable Screen parent) {
        Validate.validState(initialised, "AKMC client core not yet initialised.");

        if (!hasShownFirstRunScreen && KEY_PAIRS.retrieveKeyNamesFromDisk().isEmpty()) {
            hasShownFirstRunScreen = true;

            minecraft.execute(
                    () -> minecraft.setScreen(new FirstRunScreen(parent != null ? parent : new TitleScreen())));
            return true;
        }

        return false;
    }
}
