package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.security.PublicKey;
import java.time.Duration;
import java.util.function.Consumer;
import javax.crypto.SecretKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class ClientLoginMixin implements ClientLoginPacketListener {
    @Unique
    private volatile ClientLoginHandler authorisedKeysMC$loginHandler;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(
            Connection connection,
            Minecraft minecraft,
            ServerData serverData,
            Screen parent,
            boolean newWorld,
            Duration worldLoadDuration,
            Consumer<Component> updateStatus,
            LevelLoadTracker levelLoadTracker,
            TransferState transferState,
            CallbackInfo ci) {
        authorisedKeysMC$loginHandler = new ClientLoginHandler(
                minecraft, (ClientHandshakePacketListenerImpl) (Object) this, connection, updateStatus);
    }

    @Inject(method = "handleCustomQuery", at = @At("HEAD"), cancellable = true)
    public void handleQuery(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
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

        authorisedKeysMC$loginHandler.setSessionHash(hash);

        return hash;
    }
}
