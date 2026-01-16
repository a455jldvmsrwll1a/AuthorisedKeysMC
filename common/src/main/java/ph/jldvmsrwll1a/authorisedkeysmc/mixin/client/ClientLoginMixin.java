package ph.jldvmsrwll1a.authorisedkeysmc.mixin.client;

import io.netty.buffer.ByteBufUtil;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.*;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.function.Consumer;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class ClientLoginMixin implements ClientLoginPacketListener {
    @Shadow
    @Final
    private Consumer<Component> updateStatus;

    @Shadow
    @Final
    private Connection connection;

    @Unique
    private Ed25519PublicKeyParameters authorisedKeysMC$serverKey;

    @Unique
    private byte[] authorisedKeysMC$nonce;

    @Unique
    private Ed25519PrivateKeyParameters authorisedKeysMC$secret;

    @Inject(method = "handleCustomQuery", at = @At("HEAD"), cancellable = true)
    public void handleQuery(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
        Constants.LOG.info("CUSTOM QUERY! TRANSACTION {} on {}", packet.transactionId(), packet.payload().id());

        if (!packet.payload().id().equals(Constants.LOGIN_CHANNEL_ID)) {
            return;
        }

        if (!(packet.payload() instanceof RetainedQueryPayload payload)) {
            Constants.LOG.warn("AKMC: received custom query with ID {} belonging to us but not carrying a RetainedQueryPayload!", packet.transactionId());
            return;
        }

        ci.cancel();

        FriendlyByteBuf buf = payload.buf();

        Constants.LOG.info("HANDLE AKMC QUERY, ID = {}", packet.transactionId());

        Constants.LOG.info(ByteBufUtil.prettyHexDump(buf));

        QueryPayloadType qpType = BaseQueryPayload.peekPayloadType(buf);
        switch (qpType) {
            case SERVER_KEY -> {
                ServerPublicKeyPayload keyPayload = new ServerPublicKeyPayload(buf);
                authorisedKeysMC$serverKey = keyPayload.key;
                Constants.LOG.info("GOT SERVER'S PUBLIC KEY: {}", Base64Util.encode(keyPayload.key.getEncoded()));

                ClientChallengePayload challengePayload = new ClientChallengePayload();
                authorisedKeysMC$nonce = challengePayload.getNonce();

                this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), challengePayload));
                this.updateStatus.accept(Component.literal("Verifying server's identity..."));
            }
            case CLIENT_CHALLENGE_RESPONSE -> {
                ServerSignaturePayload signaturePayload = new ServerSignaturePayload(buf);
                Constants.LOG.info("GOT SERVER'S SIGNATURE: {}", Base64Util.encode(signaturePayload.signature));

                if (authorisedKeysMC$serverKey == null) {
                    var err = "Got a challenge response signature from the server before its key could be known!";
                    Constants.LOG.error(err);
                    this.connection.disconnect(Component.literal(err));

                    return;
                }

                if (authorisedKeysMC$nonce == null) {
                    var err = "Got a bogus challenge response from the server!";
                    Constants.LOG.error(err);
                    this.connection.disconnect(Component.literal(err));

                    return;
                }

                if (!signaturePayload.verify(authorisedKeysMC$serverKey, authorisedKeysMC$nonce)) {
                    Constants.LOG.warn("Failed to verify signature from server!");
                    this.connection.disconnect(Component.literal("The server could not prove its identity! (incorrect signature)"));

                    return;
                }

                try {
                    authorisedKeysMC$secret = new Ed25519PrivateKeyParameters(SecureRandom.getInstanceStrong());
                    Ed25519PublicKeyParameters pub = authorisedKeysMC$secret.generatePublicKey();
                    Constants.LOG.info("SIG OK! Sending random pubkey: {}", Base64Util.encode(pub.getEncoded()));
                    this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), new ClientKeyPayload(pub)));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                this.updateStatus.accept(Component.literal("Waiting for signature challenge from the server..."));
            }
            case SERVER_CHALLENGE -> {
                ServerChallengePayload challengePayload = new ServerChallengePayload(buf);
                Constants.LOG.info("SERVER WANTS US TO SIGN THIS: {}", Base64Util.encode(challengePayload.getNonce()));

                this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), ClientSignaturePayload.fromSigningChallenge(authorisedKeysMC$secret, challengePayload)));
                this.updateStatus.accept(Component.literal("Waiting for authentication verdict..."));
            }
            case SERVER_KEY_REJECTION -> {
                try {
                    authorisedKeysMC$secret = new Ed25519PrivateKeyParameters(SecureRandom.getInstanceStrong());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                Ed25519PublicKeyParameters pub = authorisedKeysMC$secret.generatePublicKey();
                Constants.LOG.info("KEY REJECTED! Sending random pubkey: {}", Base64Util.encode(pub.getEncoded()));
                this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), new ClientKeyPayload(pub)));
                this.updateStatus.accept(Component.literal("Key rejected! Trying next key..."));
            }
        }
    }
}
