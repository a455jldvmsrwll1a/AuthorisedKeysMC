package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientHandshakePacketListenerImpl.class)
public interface ClientHandshakePacketListenerAccessorMixin {
    @Accessor("serverData")
    @Nullable
    ServerData getServerData();
}
