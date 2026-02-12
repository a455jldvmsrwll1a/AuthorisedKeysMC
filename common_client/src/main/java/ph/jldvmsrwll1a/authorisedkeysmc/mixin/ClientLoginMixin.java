package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.security.PublicKey;
import java.util.function.Consumer;
import javax.crypto.SecretKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class ClientLoginMixin implements ClientLoginPacketListener {
    @Shadow
    @Final
    private Connection connection;

    @Shadow
    @Final
    private Consumer<Component> updateStatus;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private volatile @Nullable ClientLoginHandler authorisedKeysMC$loginHandler;

    @Inject(method = "handleHello", at = @At("HEAD"))
    private void spawnLoginHandler(ClientboundHelloPacket packet, CallbackInfo ci) {
        Validate.validState(authorisedKeysMC$loginHandler == null, "Login handler already spawned.");

        authorisedKeysMC$loginHandler = new ClientLoginHandler(
                minecraft, (ClientHandshakePacketListenerImpl) (Object) this, connection, updateStatus);
    }

    @WrapOperation(
            method = "handleHello",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/util/Crypt;digestData(Ljava/lang/String;Ljava/security/PublicKey;Ljavax/crypto/SecretKey;)[B"))
    private byte[] extractSessionHash(
            String serverId, PublicKey publicKey, SecretKey secretKey, Operation<byte[]> original) {
        byte[] hash = original.call(serverId, publicKey, secretKey);

        ClientLoginHandler handler = authorisedKeysMC$loginHandler;
        Validate.validState(handler != null, "Login handler not yet spawned.");
        handler.setSessionHash(hash);

        return hash;
    }

    @Inject(method = "handleCustomQuery", at = @At("HEAD"), cancellable = true)
    private void handleQuery(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
        if (!packet.payload().id().equals(Constants.LOGIN_CHANNEL_ID)) {
            return;
        }

        ci.cancel();

        ClientLoginHandler handler = authorisedKeysMC$loginHandler;

        if (handler == null) {
            throw new IllegalStateException("ClientLoginHandler is null but should have been instantiated!");
        }

        handler.handleRawMessage(packet.payload(), packet.transactionId());
    }
}
