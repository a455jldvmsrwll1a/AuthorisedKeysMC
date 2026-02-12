package ph.jldvmsrwll1a.authorisedkeysmc.net;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.*;

public final class ServerLoginHandler {
    // TODO: make this configurable
    final boolean allowsUnregistered = true;
    // TODO: make this configurable
    final boolean registrationRequired = false;

    private final ServerLoginPacketListenerImpl listener;
    private final Connection connection;
    private final GameProfile profile;
    private final byte[] sessionHash;
    private final ConcurrentLinkedQueue<BaseC2SPayload> inbox;

    private final Ed25519PrivateKeyParameters signingKey = AuthorisedKeysModCore.SERVER_KEYPAIR.secretKey;
    private final Ed25519PublicKeyParameters serverKey = AuthorisedKeysModCore.SERVER_KEYPAIR.publicKey;

    private int txId = 0;
    private Phase phase = Phase.SEND_SERVER_KEY;
    private int ticksLeft = 300;

    private @Nullable Ed25519PublicKeyParameters clientKey;
    private byte @Nullable [] nonce;

    public ServerLoginHandler(
            @NotNull ServerLoginPacketListenerImpl listener,
            @NotNull Connection connection,
            @NotNull GameProfile profile,
            byte @NotNull [] sessionHash) {
        Validate.isTrue(connection.isEncrypted(), "Connection must already be encrypted before AKMC auth may proceed!");

        this.listener = listener;
        this.connection = connection;
        this.profile = profile;
        this.sessionHash = sessionHash;

        inbox = new ConcurrentLinkedQueue<>();
    }

    public boolean finished() {
        return phase == Phase.SUCCESSFUL;
    }

    public Sender getSender() {
        return new Sender();
    }

    public void tick() {
        ticksLeft--;

        if (ticksLeft <= 0) {
            if (phase == Phase.WAIT_FOR_CLIENT_REGISTRATION_KEY || phase == Phase.WAIT_FOR_CLIENT_REGISTRATION_SIGNATURE) {
                listener.disconnect(Component.literal("Took too long to register!"));
            } else {
                listener.disconnect(Component.literal("Took too long to authenticate!"));
            }
        }

        if (phase == Phase.SEND_SERVER_KEY) {
            send(new S2CPublicKeyPayload(serverKey));
            transition(Phase.WAIT_FOR_CLIENT_CHALLENGE);
        }

        handleMessages();
    }

    private void handleMessages() {
        while (true) {
            BaseC2SPayload payload = inbox.poll();

            if (payload == null) {
                break;
            }

            switch (payload) {
                case C2SChallengePayload p -> handleChallenge(p);
                case C2SIdAckPayload p -> handleAcknowledgement(p);
                case C2SPublicKeyPayload p -> handleKey(p);
                case C2SSignaturePayload p -> handleSignature(p);
                case C2SRefuseRegistrationPayload p -> handleRegistrationRefusal(p);
                default ->
                        throw new IllegalArgumentException("Unknown C2S payload type of %s!"
                                .formatted(payload.getClass().getName()));
            }
        }
    }

    private void handleChallenge(C2SChallengePayload payload) {
        Validate.validState(
                phase.equals(Phase.WAIT_FOR_CLIENT_CHALLENGE),
                "Received client challenge but wasn't expecting one!");

        send(S2CSignaturePayload.fromSigningChallenge(signingKey, payload, sessionHash));
        transition(Phase.WAIT_FOR_ACK);
    }

    private void handleAcknowledgement(C2SIdAckPayload payload) {
        Validate.validState(
                phase.equals(Phase.WAIT_FOR_ACK),
                "Received acknowledgement but wasn't expecting it!");

        if (AuthorisedKeysModCore.USER_KEYS.userHasAnyKeys(profile.id())) {
            send(new S2CAuthenticationRequestPayload());
            transition(Phase.WAIT_FOR_CLIENT_AUTHENTICATION_KEY);
        } else if (allowsUnregistered) {
            send(new S2CRegistrationRequestPayload(registrationRequired));
            transition(Phase.WAIT_FOR_CLIENT_REGISTRATION_KEY);
        } else {
            listener.disconnect(Component.translatable("authorisedkeysmc.error.must-preregister"));
        }
    }

    private void handleKey(C2SPublicKeyPayload payload) {
        clientKey = payload.key;

        if (phase == Phase.WAIT_FOR_CLIENT_AUTHENTICATION_KEY) {
            if (!AuthorisedKeysModCore.USER_KEYS.userHasKey(profile.id(), clientKey)) {
                listener.disconnect(Component.translatable("authorisedkeysmc.error.key-rejected"));

                return;
            }

            S2CChallengePayload challenge = new S2CChallengePayload();
            nonce = challenge.getNonce();
            send(challenge);
            transition(Phase.WAIT_FOR_CLIENT_AUTHENTICATION_SIGNATURE);
        } else if (phase == Phase.WAIT_FOR_CLIENT_REGISTRATION_KEY) {
            S2CChallengePayload challenge = new S2CChallengePayload();
            nonce = challenge.getNonce();
            send(challenge);
            transition(Phase.WAIT_FOR_CLIENT_REGISTRATION_SIGNATURE);
        } else {
            throw new IllegalStateException("Received client public key but wasn't expecting one!");
        }
    }

    private void handleSignature(C2SSignaturePayload payload) {
        Validate.notNull(nonce, "Received client signature for a non-existent challenge.");

        if (phase == Phase.WAIT_FOR_CLIENT_AUTHENTICATION_SIGNATURE) {
            if (!payload.verify(clientKey, nonce, sessionHash, false)) {
                listener.disconnect(Component.translatable("authorisedkeysmc.error.invalid-signature"));

                return;
            }

            Constants.LOG.info("Successfully verified {}'s identity!", profile.name());
            transition(Phase.SUCCESSFUL);
        } else if (phase == Phase.WAIT_FOR_CLIENT_REGISTRATION_SIGNATURE) {
            if (!payload.verify(clientKey, nonce, sessionHash, true)) {
                listener.disconnect(Component.translatable("authorisedkeysmc.error.invalid-signature"));

                return;
            }

            AuthorisedKeysModCore.USER_KEYS.bindKey(profile.id(), profile.id(), clientKey);
            Constants.LOG.info("Successfully registered {}'s key!", profile.name());
            transition(Phase.SUCCESSFUL);
        } else {
            throw new IllegalStateException("Received client signature but wasn't expecting one!");
        }
    }

    private void handleRegistrationRefusal(C2SRefuseRegistrationPayload payload) {
        Validate.validState(phase == Phase.WAIT_FOR_CLIENT_REGISTRATION_KEY, "Received bogus registration refusal.");

        if (registrationRequired) {
            Constants.LOG.info("{} refuses to register!", profile.name());
            listener.disconnect(Component.translatable("authorisedkeysmc.error.registration-mandatory"));

            return;
        }

        Constants.LOG.warn("{} has successfully logged in but is unregistered!", profile.name());
        transition(Phase.SUCCESSFUL);
    }

    private synchronized void send(CustomQueryPayload payload) {
        if (connection != null) {
            connection.send(new ClientboundCustomQueryPacket(txId, payload));
            txId++;
        }
    }

    private void transition(Phase phase) {
        Constants.LOG.info("{}: {} --> {}", profile.name(), this.phase, phase);
        this.phase = phase;
    }

    public class Sender {
        public void receive(@NotNull BaseC2SPayload payload) {
            inbox.add(payload);
        }

        public void receive(@NotNull FriendlyByteBuf buf) {
            QueryAnswerPayloadType kind = BaseC2SPayload.peekPayloadType(buf);
            BaseC2SPayload base =
                    switch (kind) {
                        case CLIENT_CHALLENGE -> new C2SChallengePayload(buf);
                        case ID_ACK -> new C2SIdAckPayload();
                        case CLIENT_KEY -> new C2SPublicKeyPayload(buf);
                        case SERVER_CHALLENGE_RESPONSE -> new C2SSignaturePayload(buf);
                        case WONT_REGISTER -> new C2SRefuseRegistrationPayload();
                    };

            receive(base);
        }

        public void receive(@NotNull CustomQueryAnswerPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            payload.write(buf);

            receive(buf);
        }
    }

    private enum Phase {
        /// When sending host key.
        SEND_SERVER_KEY,
        /// After host key is sent and waiting for client to challenge us.
        WAIT_FOR_CLIENT_CHALLENGE,
        /// After signature is sent and waiting for an acknowledgement.
        WAIT_FOR_ACK,
        /// After sending authentication request and waiting for client's key.
        WAIT_FOR_CLIENT_AUTHENTICATION_KEY,
        /// After sending registration request and waiting for client's key.
        WAIT_FOR_CLIENT_REGISTRATION_KEY,
        /// After challenging client for authentication and waiting for response.
        WAIT_FOR_CLIENT_AUTHENTICATION_SIGNATURE,
        /// After challenging client for authentication and waiting for response.
        WAIT_FOR_CLIENT_REGISTRATION_SIGNATURE,
        /// Nothing more to do.
        SUCCESSFUL,
    }
}
