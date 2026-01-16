package ph.jldvmsrwll1a.authorisedkeysmc.mixin.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.RetainedQueryPayload;

@Mixin(ClientboundCustomQueryPacket.class)
public abstract class CustomQueryPacketMixin {
    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
    private static void preservePayloadBuffer(Identifier id, FriendlyByteBuf buffer, CallbackInfoReturnable<CustomQueryPayload> cir) {
        if (!id.equals(Constants.LOGIN_CHANNEL_ID)) {
            // We do not care about messages not meant for us.
            return;
        }

        int len = buffer.readableBytes();

        if (len < 0 || len > 1024) {
            throw new IllegalArgumentException("AKMC payload shall not exceed 1 KiB");
        }

        cir.setReturnValue(new RetainedQueryPayload(id, new FriendlyByteBuf(buffer.copy())));

        // We must skip bytes to empty the buffer. Otherwise, Minecraft will complain about the left-over bytes.
        buffer.skipBytes(len);
    }
}
