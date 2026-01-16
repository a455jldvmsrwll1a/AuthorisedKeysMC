package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.RetainedQueryAnswerPayload;

@Mixin(value = ServerboundCustomQueryAnswerPacket.class, priority = 10000)
public abstract class CustomQueryAnswerPacketMixin {
    // Fabric has its own payload class, so this might not get applied.
    // Still fine though since it doesn't discard the contents either.
    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
    private static void preservePayloadBuffer(int transactionId, FriendlyByteBuf buffer, CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        boolean hasPayload = buffer.readBoolean();
        int len = buffer.readableBytes();

        if (len < 0 || len > 1048576) {
            throw new IllegalArgumentException("Payload shall not exceed 1 MiB");
        }

        if (hasPayload) {
            cir.setReturnValue(new RetainedQueryAnswerPayload(new FriendlyByteBuf(buffer.copy())));
        } else {
            cir.setReturnValue(null);
        }

        // We must skip bytes to empty the buffer. Otherwise, Minecraft will complain about the left-over bytes.
        buffer.skipBytes(len);
    }
}
