package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.notifications.ServerActivityMonitor;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ServerLoginHandler;
import ph.jldvmsrwll1a.authorisedkeysmc.net.VanillaLoginHandlerState;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;

import javax.crypto.SecretKey;
import java.security.PublicKey;

// Use lower priority so that we run before any mod-loader-loaded mixin runs.
@Mixin(value = ServerLoginPacketListenerImpl.class, priority = 500)
public abstract class ServerLoginMixin implements ServerLoginPacketListener, TickablePacketListener {
    @Unique @Nullable
    private volatile ServerLoginHandler authorisedKeysMC$loginHandler = null;

    @Shadow
    @Final
    Connection connection;

    @Shadow
    public abstract void disconnect(Component p_10054_);

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    private int tick;

    @Shadow
    @Nullable String requestedUsername;

    @Shadow
    abstract void startClientVerification(GameProfile authenticatedProfile);

    @Shadow
    @Final
    private byte[] challenge;

    @Shadow
    @Final
    ServerActivityMonitor serverActivityMonitor;

    @Shadow
    private @Nullable GameProfile authenticatedProfile;

    @Unique
    private boolean authorisedKeysMC$skipped = false;

    @Unique
    private byte @Nullable [] authorisedKeysMC$sessionHash;

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void handleIncoming(ServerboundHelloPacket packet, CallbackInfo ci) {
        requestedUsername = packet.name();

        if (server.isSingleplayer()) {
            // Mark as finished so that verification actually happens.
            authorisedKeysMC$skipLogin();
            return;
        }
        ci.cancel();

        IPlatformHelper platform = AuthorisedKeysModCore.PLATFORM;
        ServerLoginPacketListenerImpl self = (ServerLoginPacketListenerImpl) (Object) this;

        Validate.validState(platform.getLoginState(self).equals(VanillaLoginHandlerState.STARTING), "Received an unexpected hello packet");
        Validate.validState(StringUtil.isValidPlayerName(packet.name()), "Invalid username");

        GameProfile spProfile = server.getSingleplayerProfile();
        if (spProfile != null && requestedUsername.equalsIgnoreCase(spProfile.name())) {
            authorisedKeysMC$skipLogin();
            startClientVerification(spProfile);
            return;
        }

        if (connection.isMemoryConnection()) {
            authorisedKeysMC$skipLogin();
            startClientVerification(UUIDUtil.createOfflineProfile(requestedUsername));
            return;
        }

        // Always encrypt even if server does not use authentication.

        if (server.usesAuthentication()) {
            Constants.LOG.info("server uses authentication");
        }

        platform.setLoginState(self, VanillaLoginHandlerState.ENCRYPTING);
        connection.send(new ClientboundHelloPacket("", server.getKeyPair().getPublic().getEncoded(), challenge, server.usesAuthentication()));
    }

    @Inject(method = "handleKey", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V"), cancellable = true)
    private void handleEncryptionKey(ServerboundKeyPacket packet, CallbackInfo ci) {
        if (!server.usesAuthentication()) {
            // Prevent the Vanilla authenticator thread from running.
            // The client has not authenticated, so we shouldn't either.
            ci.cancel();

            Validate.notNull(requestedUsername, "Requested username has not yet been set! This is a bug.");
            startClientVerification(UUIDUtil.createOfflineProfile(requestedUsername));
        }
    }

    @WrapOperation(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;digestData(Ljava/lang/String;Ljava/security/PublicKey;Ljavax/crypto/SecretKey;)[B"))
    private byte[] extractSessionHash(String serverId, PublicKey publicKey, SecretKey secretKey, Operation<byte[]> original) {
        byte[] hash = original.call(serverId, publicKey, secretKey);

        authorisedKeysMC$sessionHash = hash;

        return hash;
    }

    @Inject(method = "startClientVerification", at = @At("HEAD"))
    private void detourFromVerification(GameProfile authenticatedProfile, CallbackInfo ci) {
        if (authorisedKeysMC$skipped || authorisedKeysMC$loginHandler != null) {
            return;
        }

        Validate.notNull(authorisedKeysMC$sessionHash, "Session hash must already by known before starting authentication.");

        // Start the custom authentication.
        Constants.LOG.info("begin custom auth");
        authorisedKeysMC$loginHandler = new ServerLoginHandler((ServerLoginPacketListenerImpl) (Object) this, connection, authenticatedProfile, authorisedKeysMC$sessionHash);
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void handleResponse(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        ServerLoginHandler loginHandler = authorisedKeysMC$loginHandler;

        if (authorisedKeysMC$skipped || loginHandler == null) {
            // Custom authentication hasn't started yet. We are not expecting a response at this time.
            return;
        }

        // Payload is null if client did not understand our custom query.
        if (packet.payload() == null) {
            if (!loginHandler.hasClientEverResponded()) {
                // Did not understand the very first query we sent. Client most likely does not have the mod installed.
                ci.cancel();
                disconnect(Component.literal("Access denied!!! D:"));
                Constants.LOG.info("{} does not have the mod installed.", requestedUsername);
            }

            return;
        }

        ci.cancel();

        loginHandler.handleRawMessage(packet.payload());
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        ServerLoginHandler loginHandler = authorisedKeysMC$loginHandler;

        if (authorisedKeysMC$skipped || loginHandler == null || loginHandler.finished()) {
            Constants.LOG.info("normal tick {}, state = {}", tick, AuthorisedKeysModCore.PLATFORM.getLoginState((ServerLoginPacketListenerImpl) (Object) this));
            return;
        }
        ci.cancel();

        loginHandler.tick(tick);

        if (loginHandler.finished()) {
            serverActivityMonitor.reportLoginActivity();
            startClientVerification(authenticatedProfile);
        }
    }

    @Unique
    private void authorisedKeysMC$skipLogin() {
        Constants.LOG.info("Skipped verifying {}'s identity!", requestedUsername);
        authorisedKeysMC$skipped = true;
    }
}
