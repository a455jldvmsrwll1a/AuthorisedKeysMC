package ph.jldvmsrwll1a.authorisedkeysmc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.ClientKeyPairs;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.PrivateKeyCache;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.FirstRunScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.servers.KeyUses;
import ph.jldvmsrwll1a.authorisedkeysmc.servers.KnownHosts;

public class AuthorisedKeysModClient {
    public static ClientKeyPairs KEY_PAIRS;
    public static KnownHosts KNOWN_HOSTS;
    public static KeyUses KEY_USES;
    public static PrivateKeyCache CACHED_KEYS;

    private static boolean hasShownFirstRunScreen = false;

    public static void init() {
        KEY_PAIRS = new ClientKeyPairs();
        KNOWN_HOSTS = new KnownHosts();
        KEY_USES = new KeyUses();
        CACHED_KEYS = new PrivateKeyCache();
    }

    public static boolean maybeShowFirstRunScreen(Minecraft minecraft, @Nullable Screen parent) {
        if (!hasShownFirstRunScreen && KEY_PAIRS.retrieveKeyNamesFromDisk().isEmpty()) {
            hasShownFirstRunScreen = true;

            minecraft.execute(() -> minecraft.setScreen(new FirstRunScreen(parent != null ? parent : new TitleScreen())));
            return true;
        }

        return false;
    }
}
