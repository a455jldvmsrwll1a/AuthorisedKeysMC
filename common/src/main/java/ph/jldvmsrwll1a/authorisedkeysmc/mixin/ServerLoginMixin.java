package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.*;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.notifications.ServerActivityMonitor;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.signers.Ed25519Signer;
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
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.*;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ServerAuthenticationPhase;
import ph.jldvmsrwll1a.authorisedkeysmc.net.VanillaLoginHandlerState;
import ph.jldvmsrwll1a.authorisedkeysmc.platform.IPlatformHelper;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

// Use lower priority so that we run before any mod-loader-loaded mixin runs.
@Mixin(value = ServerLoginPacketListenerImpl.class, priority = 500)
public abstract class ServerLoginMixin implements ServerLoginPacketListener, TickablePacketListener {
    @Unique
    private int authorisedKeysMC$queryTransactionId = 0;

    @Unique
    private ServerAuthenticationPhase authorisedKeysMC$authPhase = ServerAuthenticationPhase.PROLOGUE;

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
    private @Nullable String requestedUsername;

    @Shadow
    abstract void startClientVerification(GameProfile authenticatedProfile);

    @Shadow
    @Final
    private byte[] challenge;

    @Shadow
    @Final
    private ServerActivityMonitor serverActivityMonitor;

    @Shadow
    private @Nullable GameProfile authenticatedProfile;

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void handleIncoming(ServerboundHelloPacket packet, CallbackInfo ci) {
        if (server.isSingleplayer()) {
            // Mark as finished so that verification actually happens.
            authorisedKeysMC$authPhase = ServerAuthenticationPhase.EPILOGUE;
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
            authorisedKeysMC$startVerifyNow(spProfile);
            return;
        }

        if (connection.isMemoryConnection()) {
            authorisedKeysMC$startVerifyNow(UUIDUtil.createOfflineProfile(requestedUsername));
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
            ci.cancel();

            Validate.notNull(requestedUsername, "Requested username has not yet been set! This is a bug.");

            startClientVerification(UUIDUtil.createOfflineProfile(requestedUsername));
        }
    }

    @Inject(method = "startClientVerification", at = @At("HEAD"))
    private void detourFromVerification(GameProfile authenticatedProfile, CallbackInfo ci) {
        if (authorisedKeysMC$authPhase.equals(ServerAuthenticationPhase.PROLOGUE)) {
            // Start the custom authentication.
            Constants.LOG.info("begin custom auth");
            authorisedKeysMC$authPhase = ServerAuthenticationPhase.SEND_SERVER_KEY;
        }
    }

    @Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
    private void handleResponse(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        // Payload is null if client did not understand our custom query.
        if (packet.payload() == null) {
            if (authorisedKeysMC$authPhase.equals(ServerAuthenticationPhase.WAIT_FOR_CLIENT_CHALLENGE)) {
                // Did not understand the very first query we sent. Client most likely does not have the mod installed.
                ci.cancel();
                disconnect(Component.literal("Access denied!!! D:"));
                Constants.LOG.info("{} does not have the mod installed.", requestedUsername);
            }

            return;
        }

        ci.cancel();

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.payload().write(buf);

        QueryAnswerPayloadType qap = BaseQueryAnswerPayload.peekPayloadType(buf);
        switch (qap) {
            case CLIENT_CHALLENGE -> {
                if (!authorisedKeysMC$authPhase.equals(ServerAuthenticationPhase.WAIT_FOR_CLIENT_CHALLENGE)) {
                    Constants.LOG.warn("Received client challenge but wasn't expecting one.");
                    disconnect(Component.literal("Unexpected client challenge."));
                }

                ClientChallengePayload challengePayload = new ClientChallengePayload(buf);
                Constants.LOG.info("Client challenges us to sign this data: {}", Base64Util.encode(challengePayload.getNonce()));

                Ed25519Signer signer = new Ed25519Signer();
                signer.init(true, AuthorisedKeysModCore.SERVER_KEYPAIR.secretKey);
                signer.update(challengePayload.getNonce(), 0, ClientChallengePayload.NONCE_LENGTH);
                byte[] signature = signer.generateSignature();

                Constants.LOG.info("signature: {}", Base64Util.encode(signature));
                authorisedKeysMC$send(new ServerSignaturePayload(signature));
                authorisedKeysMC$authPhase = ServerAuthenticationPhase.WAIT_FOR_CLIENT_KEY;
            }
            case CLIENT_KEY -> {
                if (!authorisedKeysMC$authPhase.equals(ServerAuthenticationPhase.WAIT_FOR_CLIENT_KEY)) {
                    Constants.LOG.warn("Received client public key but wasn't expecting one.");
                    disconnect(Component.literal("Unexpected client public key."));
                }

                ClientKeyPayload keyPayload = new ClientKeyPayload(buf);
                Constants.LOG.info("Client has given us their public key: {}", Base64Util.encode(keyPayload.key.getEncoded()));
                Constants.LOG.info("YOU GO IN NOW!!!!!!!!!!!!!");
                authorisedKeysMC$authPhase = ServerAuthenticationPhase.SUCCESSFUL;
            }
            case SERVER_CHALLENGE_RESPONSE -> {
                disconnect(Component.literal("SERVER_CHALLENGE_RESPONSE??????????"));
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (!authorisedKeysMC$authPhase.shouldHandleTick()) {
            Constants.LOG.info("normal tick {}, state = {}", tick, AuthorisedKeysModCore.PLATFORM.getLoginState((ServerLoginPacketListenerImpl) (Object) this));
            return;
        }
        ci.cancel();

        Constants.LOG.info("custom auth tick {}, phase = {}", tick, authorisedKeysMC$authPhase);

        if (tick++ >= 600) {
            disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
            return;
        }

        switch (authorisedKeysMC$authPhase) {
            case SEND_SERVER_KEY -> {
                authorisedKeysMC$send(new ServerPublicKeyPayload(AuthorisedKeysModCore.SERVER_KEYPAIR.publicKey));
                authorisedKeysMC$authPhase = ServerAuthenticationPhase.WAIT_FOR_CLIENT_CHALLENGE;
            }
            case WAIT_FOR_CLIENT_CHALLENGE, WAIT_FOR_CLIENT_KEY -> {
                /* do nothing */
            }
            case SUCCESSFUL -> {
                Constants.LOG.info("Successfully verified {}'s identity!", authenticatedProfile.name());
                authorisedKeysMC$startVerifyNow(authenticatedProfile);
            }
            case BYPASSED -> {
                Constants.LOG.info("Skipped verifying {}'s identity!", authenticatedProfile.name());
                authorisedKeysMC$startVerifyNow(authenticatedProfile);
            }
            default -> {
                return;
            }
        }
    }

    @Unique
    private void authorisedKeysMC$startVerifyNow(GameProfile profile) {
        authorisedKeysMC$authPhase = ServerAuthenticationPhase.EPILOGUE;
        serverActivityMonitor.reportLoginActivity();
        startClientVerification(profile);
    }

    @Unique
    private void authorisedKeysMC$send(FriendlyByteBuf buf) {
        authorisedKeysMC$send(new RetainedQueryPayload(Constants.LOGIN_CHANNEL_ID, buf));
    }

    @Unique
    private void authorisedKeysMC$send(CustomQueryPayload payload) {
        connection.send(new ClientboundCustomQueryPacket(authorisedKeysMC$queryTransactionId, payload));
        authorisedKeysMC$queryTransactionId++;
    }
}

