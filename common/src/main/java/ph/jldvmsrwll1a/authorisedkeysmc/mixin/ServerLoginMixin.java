package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

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

// Use lower priority so that we run before any mod-loader-loaded mixin runs.
@Mixin(value = ServerLoginPacketListenerImpl.class, priority = 500)
public abstract class ServerLoginMixin implements ServerLoginPacketListener, TickablePacketListener {
    @Unique @Nullable
    private ServerLoginHandler authorisedKeysMC$loginHandler = null;

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

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void handleIncoming(ServerboundHelloPacket packet, CallbackInfo ci) {
        if (server.isSingleplayer()) {
            // Mark as finished so that verification actually happens.
            authorisedKeysMC$loginHandler = ServerLoginHandler.bypassedLogin();
            return;
        }
        ci.cancel();

        IPlatformHelper platform = AuthorisedKeysModCore.PLATFORM;
        ServerLoginPacketListenerImpl self = (ServerLoginPacketListenerImpl) (Object) this;

        Validate.validState(platform.getLoginState(self).equals(VanillaLoginHandlerState.STARTING), "Received an unexpected hello packet");
        Validate.validState(StringUtil.isValidPlayerName(packet.name()), "Invalid username");

        requestedUsername = packet.name();

        GameProfile spProfile = server.getSingleplayerProfile();
        if (spProfile != null && requestedUsername.equalsIgnoreCase(spProfile.name())) {
            authorisedKeysMC$loginHandler = ServerLoginHandler.bypassedLogin();
            startClientVerification(spProfile);
            return;
        }

        if (connection.isMemoryConnection()) {
            authorisedKeysMC$loginHandler = ServerLoginHandler.bypassedLogin();
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

    @Inject(method = "startClientVerification", at = @At("RETURN"))
    private void detourFromVerification(GameProfile authenticatedProfile, CallbackInfo ci) {
        // We let the original function run first.
        // This fills in authenticatedProfile and sets the state to verifying.

        if (authorisedKeysMC$loginHandler == null || authorisedKeysMC$loginHandler.finished()) {
            // Start the custom authentication.
            Constants.LOG.info("begin custom auth");
            authorisedKeysMC$loginHandler = new ServerLoginHandler((ServerLoginPacketListenerImpl) (Object) this, connection, authenticatedProfile);
        }
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void handleResponse(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (authorisedKeysMC$loginHandler == null) {
            // Custom authentication hasn't started yet. We are not expecting a response at this time.
            return;
        }

        // Payload is null if client did not understand our custom query.
        if (packet.payload() == null) {
            if (!authorisedKeysMC$loginHandler.hasClientEverResponded()) {
                // Did not understand the very first query we sent. Client most likely does not have the mod installed.
                ci.cancel();
                disconnect(Component.literal("Access denied!!! D:"));
                Constants.LOG.info("{} does not have the mod installed.", requestedUsername);
            }

            return;
        }

        ci.cancel();

        authorisedKeysMC$loginHandler.handleRawMessage(packet.payload());
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (authorisedKeysMC$loginHandler == null || authorisedKeysMC$loginHandler.finished()) {
            Constants.LOG.info("normal tick {}, state = {}", tick, AuthorisedKeysModCore.PLATFORM.getLoginState((ServerLoginPacketListenerImpl) (Object) this));
            return;
        }
        ci.cancel();

        if (tick++ >= 600) {
            disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
            return;
        }

        authorisedKeysMC$loginHandler.tick(tick);

        if (authorisedKeysMC$loginHandler.finished()) {
            serverActivityMonitor.reportLoginActivity();
            startClientVerification(authenticatedProfile);
        }
    }
}

