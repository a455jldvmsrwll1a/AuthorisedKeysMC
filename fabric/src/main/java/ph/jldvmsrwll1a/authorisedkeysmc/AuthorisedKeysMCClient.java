package ph.jldvmsrwll1a.authorisedkeysmc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class AuthorisedKeysMCClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        AuthorisedKeysModClient.init();
    }

    private void onTick(Minecraft minecraft) {
        AuthorisedKeysModClient.tick();
    }
}
