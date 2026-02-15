package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.UUID;
import javax.crypto.SecretKey;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.notifications.ServerActivityMonitor;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
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
    @Shadow
    @Final
    Connection connection;

    @Shadow
    public abstract void disconnect(Component p_10054_);

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Nullable
    String requestedUsername;

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
    @Nullable
    private ServerLoginHandler authorisedKeysMC$loginHandler = null;

    @Unique
    private ServerLoginHandler.@Nullable Sender authorisedKeysMC$handlerQueue = null;

    @Unique
    private boolean authorisedKeysMC$clientHasEverResponded = false;

    @Unique
    private volatile boolean authorisedKeysMC$skipped = false;

    @Unique
    private byte @Nullable [] authorisedKeysMC$sessionHash;

    @Unique
    private @Nullable Component authorisedKeysMC$disconnectReason = null;

    @Unique
    private boolean authorisedKeysMC$alreadyCheckedIfCanJoin = false;

    @Unique
    private boolean authorisedKeysMC$shouldUseVanillaAuthentication = false;

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

        Validate.validState(
                platform.getLoginState(self).equals(VanillaLoginHandlerState.STARTING),
                "Received an unexpected hello packet");
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

        if (server.usesAuthentication()) {
            authorisedKeysMC$shouldUseVanillaAuthentication = true;
        } else if (AuthorisedKeysModCore.CONFIG.skipOnlineAccounts) {
            UUID uuid = packet.profileId();

            if (uuid.version() == 4) {
                UUID offlineId = UUIDUtil.createOfflinePlayerUUID(packet.name());

                if (!uuid.equals(offlineId)) {
                    // Client is *probably* not offline.
                    authorisedKeysMC$shouldUseVanillaAuthentication = true;
                    authorisedKeysMC$skipLogin();
                }
            }
        }

        // Always encrypt even if server does not use authentication.

        if (authorisedKeysMC$shouldUseVanillaAuthentication) {
            Constants.LOG.info("server uses authentication");
        }

        platform.setLoginState(self, VanillaLoginHandlerState.ENCRYPTING);
        connection.send(new ClientboundHelloPacket(
                "",
                server.getKeyPair().getPublic().getEncoded(),
                challenge,
                authorisedKeysMC$shouldUseVanillaAuthentication));
    }

    @Inject(method = "handleKey", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V"), cancellable = true)
    private void handleEncryptionKey(ServerboundKeyPacket packet, CallbackInfo ci) {
        if (!authorisedKeysMC$shouldUseVanillaAuthentication) {
            // Prevent the Vanilla authenticator thread from running.
            // The client has not authenticated, so we shouldn't either.
            ci.cancel();

            Validate.notNull(requestedUsername, "Requested username has not yet been set! This is a bug.");
            startClientVerification(UUIDUtil.createOfflineProfile(requestedUsername));
        }
    }

    @WrapOperation(
            method = "handleKey",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/util/Crypt;digestData(Ljava/lang/String;Ljava/security/PublicKey;Ljavax/crypto/SecretKey;)[B"))
    private byte[] extractSessionHash(
            String serverId, PublicKey publicKey, SecretKey secretKey, Operation<byte[]> original) {
        byte[] hash = original.call(serverId, publicKey, secretKey);

        authorisedKeysMC$sessionHash = hash;

        return hash;
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void handleResponse(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (authorisedKeysMC$handlerQueue == null) {
            // Custom authentication hasn't started yet. We are not expecting a response at this time.
            return;
        }

        // Payload is null if client did not understand our custom query.
        if (packet.payload() == null) {
            if (!authorisedKeysMC$clientHasEverResponded) {
                // Did not understand the very first query we sent. Client most likely does not have the mod installed.
                ci.cancel();
                disconnect(Component.literal("Access denied!!! D:"));
                Constants.LOG.info("{} does not have the mod installed.", requestedUsername);
            }

            return;
        }

        ci.cancel();

        authorisedKeysMC$clientHasEverResponded = true;
        authorisedKeysMC$handlerQueue.receive(packet.payload());
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (!authorisedKeysMC$skipped && authorisedKeysMC$loginHandler == null && authenticatedProfile != null) {
            if (!AuthorisedKeysModCore.CONFIG.enforcing) {
                authorisedKeysMC$skipped = true;
                Constants.LOG.warn(
                        "Not verifying {}'s identity because the mod is on standby!", authenticatedProfile.name());

                return;
            }

            // Ensure that the player is actually allowed in the server as far as vanilla is concerned.
            PlayerList playerList = server.getPlayerList();
            authorisedKeysMC$disconnectReason =
                    playerList.canPlayerLogin(connection.getRemoteAddress(), new NameAndId(authenticatedProfile));
            authorisedKeysMC$alreadyCheckedIfCanJoin = true;

            if (authorisedKeysMC$disconnectReason != null) {
                ci.cancel();
                disconnect(authorisedKeysMC$disconnectReason);
                return;
            }

            Validate.notNull(
                    authorisedKeysMC$sessionHash, "Session hash must already by known before starting authentication.");

            // Start the custom authentication.
            Constants.LOG.info("begin custom auth");
            authorisedKeysMC$loginHandler = new ServerLoginHandler(
                    (ServerLoginPacketListenerImpl) (Object) this,
                    connection,
                    authenticatedProfile,
                    authorisedKeysMC$sessionHash);

            authorisedKeysMC$handlerQueue = authorisedKeysMC$loginHandler.getSender();
        }

        boolean bypassed = authorisedKeysMC$skipped && authorisedKeysMC$loginHandler == null;
        boolean preAuth = !authorisedKeysMC$skipped && authorisedKeysMC$loginHandler == null;
        boolean finished = authorisedKeysMC$loginHandler != null && authorisedKeysMC$loginHandler.finished();

        if (bypassed || finished) {
            return;
        } else if (preAuth) {
            // If authenticatedProfile was set at this point, vanilla login would proceed without the custom
            // authentication ever stepping in. Obviously a very bad outcome.
            if (authenticatedProfile != null) {
                throw new IllegalStateException("Custom authentication handler did not get created on time!");
            }

            return;
        }

        ci.cancel();

        authorisedKeysMC$loginHandler.tick();

        if (authorisedKeysMC$loginHandler.finished()) {
            serverActivityMonitor.reportLoginActivity();
        }
    }

    @WrapOperation(
            method = "verifyLoginAndFinishConnectionSetup",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;"))
    private Component useCachedDisconnectReason(
            PlayerList instance, SocketAddress address, NameAndId nameAndId, Operation<Component> original) {
        if (authorisedKeysMC$alreadyCheckedIfCanJoin) {
            return authorisedKeysMC$disconnectReason;
        }

        return original.call(instance, address, nameAndId);
    }

    @Unique
    private void authorisedKeysMC$skipLogin() {
        Validate.validState(authorisedKeysMC$loginHandler == null, "Login handler should not exist.");

        Constants.LOG.info("Skipped verifying {}'s identity!", requestedUsername);
        authorisedKeysMC$skipped = true;
    }
}
