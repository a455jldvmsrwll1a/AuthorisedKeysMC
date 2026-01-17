package ph.jldvmsrwll1a.authorisedkeysmc.net;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
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
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.*;

public final class ServerLoginHandler {
    private final ServerLoginPacketListenerImpl listener;
    private final Connection connection;
    private final GameProfile profile;

    private final Ed25519PrivateKeyParameters signingKey = AuthorisedKeysModCore.SERVER_KEYPAIR.secretKey;
    private final Ed25519PublicKeyParameters serverKey = AuthorisedKeysModCore.SERVER_KEYPAIR.publicKey;

    private volatile int txId = 0;
    private volatile Phase phase = Phase.SEND_SERVER_KEY;
    private int ticksLeft = 300;
    private int triesLeft = 15; // TODO: should be however many keys can be registered at maximum

    private Ed25519PublicKeyParameters currentKey;
    private byte[] nonce;

    public ServerLoginHandler(ServerLoginPacketListenerImpl listener, Connection connection, GameProfile profile) {
        Validate.isTrue(connection == null || connection.isEncrypted(), "Connection must already be encrypted before AKMC auth may proceed!");

        this.listener = listener;
        this.connection = connection;
        this.profile = profile;
    }

    public static ServerLoginHandler bypassedLogin() {
        Constants.LOG.info("Skipped verifying identity!");

        ServerLoginHandler handler = new ServerLoginHandler(null, null, null);
        handler.transition(Phase.SUCCESSFUL);
        return handler;
    }

    public boolean finished() {
        return phase == Phase.SUCCESSFUL;
    }

    public boolean hasClientEverResponded() {
        return phase != Phase.SEND_SERVER_KEY && phase != Phase.WAIT_FOR_CLIENT_CHALLENGE;
    }

    public void tick(int tick) {
        Constants.LOG.info("auth tick {}, phase = {}", tick, phase);

        switch (phase) {
            case WAIT_FOR_CLIENT_CHALLENGE, WAIT_FOR_CLIENT_KEY, WAIT_FOR_CLIENT_SIGNATURE -> {
                ticksLeft--;

                if (ticksLeft <= 0) {
                    listener.disconnect(Component.literal("Took too long to authenticate!"));
                }
            }
            case SEND_SERVER_KEY -> {
                send(new S2CPublicKeyPayload(serverKey));
                transition(Phase.WAIT_FOR_CLIENT_CHALLENGE);
            }
            case SUCCESSFUL -> {
                // Do nothing. We are done.
            }
        }
    }

    public void handleMessage(BaseC2SPayload payload) {
        switch (payload) {
            case C2SChallengePayload challengePayload -> {
                Validate.validState(phase.equals(Phase.WAIT_FOR_CLIENT_CHALLENGE), "Received client challenge but wasn't expecting one!");

                send(S2CSignaturePayload.fromSigningChallenge(signingKey, challengePayload));
                transition(Phase.WAIT_FOR_CLIENT_KEY);
            }
            case C2SPublicKeyPayload keyPayload -> {
                Validate.validState(phase.equals(Phase.WAIT_FOR_CLIENT_KEY), "Received client public key but wasn't expecting one!");

                currentKey = keyPayload.key;

                S2CChallengePayload challenge = new S2CChallengePayload();
                nonce = challenge.getNonce();
                send(challenge);
                transition(Phase.WAIT_FOR_CLIENT_SIGNATURE);
            }
            case C2SSignaturePayload signaturePayload -> {
                Validate.validState(phase.equals(Phase.WAIT_FOR_CLIENT_SIGNATURE), "Received client signature but wasn't expecting one!");

                if (signaturePayload.verify(currentKey, nonce)) {
                    Constants.LOG.info("Successfully verified {}'s identity!", profile.name());
                    transition(Phase.SUCCESSFUL);
                } else {
                    send(new S2CKeyRejectedPayload());
                    transition(Phase.WAIT_FOR_CLIENT_KEY);

                    triesLeft--;
                    if (triesLeft <= 0) {
                        listener.disconnect(Component.literal("Too many attempts to authenticate!"));
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unknown base query answer payload type of %s!".formatted(payload.getClass().getName()));
        }
    }

    public void handleRawMessage(FriendlyByteBuf buf) {
        QueryAnswerPayloadType kind = BaseC2SPayload.peekPayloadType(buf);
        BaseC2SPayload base = switch (kind) {
            case CLIENT_CHALLENGE -> new C2SChallengePayload(buf);
            case CLIENT_KEY -> new C2SPublicKeyPayload(buf);
            case SERVER_CHALLENGE_RESPONSE -> new C2SSignaturePayload(buf);
        };

        handleMessage(base);
    }

    public void handleRawMessage(CustomQueryAnswerPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);

        handleRawMessage(buf);
    }

    private synchronized void send(CustomQueryPayload payload) {
        if (connection != null) {
            connection.send(new ClientboundCustomQueryPacket(txId, payload));
            txId++;
        }
    }

    private VanillaLoginHandlerState getState() {
        return AuthorisedKeysModCore.PLATFORM.getLoginState(listener);
    }

    private void setState(VanillaLoginHandlerState state) {
        AuthorisedKeysModCore.PLATFORM.setLoginState(listener, state);
    }

    private void transition(Phase phase) {
        Constants.LOG.info("{}: {} --> {}", profile.name(), this.phase, phase);
        this.phase = phase;
    }

    private enum Phase {
        SEND_SERVER_KEY,
        WAIT_FOR_CLIENT_CHALLENGE,
        WAIT_FOR_CLIENT_KEY,
        WAIT_FOR_CLIENT_SIGNATURE,
        SUCCESSFUL,
    }
}
