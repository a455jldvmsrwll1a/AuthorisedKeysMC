package ph.jldvmsrwll1a.authorisedkeysmc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.FabricPlatformHelper;

public class AuthorisedKeysMCClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        AkmcClient.init(new FabricPlatformHelper());
    }

    private void onTick(Minecraft minecraft) {
        AkmcClient.tick();
    }
}
