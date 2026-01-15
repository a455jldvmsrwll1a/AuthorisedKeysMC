package ph.jldvmsrwll1a.authorisedkeysmc;

import io.netty.buffer.ByteBufUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.impl.networking.payload.PacketByteBufLoginQueryResponse;
import net.minecraft.network.chat.Component;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.FabricPlatformHelper;

public class AuthorisedKeysMC implements ModInitializer {
    
    @Override
    public void onInitialize() {
        AuthorisedKeysModCore.init(new FabricPlatformHelper());
    }
}
