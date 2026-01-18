package ph.jldvmsrwll1a.authorisedkeysmc.net.client;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.client.gui.LoginRegistrationScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.client.gui.UnknownServerKeyWarningScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.client.gui.WrongServerKeyWarningScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.mixin.client.ClientHandshakePacketListenerAccessorMixin;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.*;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public final class ClientLoginHandler {
    private final Minecraft minecraft;
    private final ClientHandshakePacketListenerImpl listener;
    private final Connection connection;
    private final Consumer<Component> updateStatus;

    private Ed25519PublicKeyParameters serverKey;
    private byte[] nonce;

    private Ed25519PrivateKeyParameters secretKey;

    boolean registering;

    private volatile int txId = -1;

    public ClientLoginHandler(Minecraft minecraft, ClientHandshakePacketListenerImpl listener, Connection connection, Consumer<Component> updateStatus) {
        this.minecraft = minecraft;
        this.listener = listener;
        this.connection = connection;
        this.updateStatus = updateStatus;
    }

    public boolean disconnected() {
        return !connection.isConnected();
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }

    public Optional<String> getServerName() {
        return Optional.ofNullable(((ClientHandshakePacketListenerAccessorMixin) listener).getServerData()).map(data -> data.name);
    }

    public void handleRawMessage(CustomQueryPayload payload, int txId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        handleRawMessage(buf, txId);
    }

    public void handleRawMessage(FriendlyByteBuf buf, int txId) {
        QueryPayloadType kind = BaseS2CPayload.peekPayloadType(buf);
        BaseS2CPayload base = switch (kind) {
            case SERVER_KEY -> new S2CPublicKeyPayload(buf);
            case CLIENT_CHALLENGE_RESPONSE -> new S2CSignaturePayload(buf);
            case SERVER_CHALLENGE -> new S2CChallengePayload(buf);
            case SERVER_KEY_REJECTION -> new S2CKeyRejectedPayload();
            case REGISTRATION_REQUEST -> new S2CRegistrationRequestPayload();
        };

        handleMessage(base, txId);
    }

    public void handleMessage(BaseS2CPayload payload, int txId) {
        this.txId = txId;

        switch (payload) {
            case S2CPublicKeyPayload serverKeyPayload -> {
                serverKey = serverKeyPayload.key;
                Constants.LOG.info("GOT SERVER'S PUBLIC KEY: {}", Base64Util.encode(serverKeyPayload.key.getEncoded()));

                Ed25519PublicKeyParameters knownKey = getServerName().map(name -> AuthorisedKeysModClient.KNOWN_SERVERS.getServerkey(name)).orElse(null);

                if (knownKey == null) {
                    minecraft.execute(() -> {
                        Screen screen = UnknownServerKeyWarningScreen.create(this, serverKey);
                        minecraft.setScreen(screen);
                    });
                } else if (!Arrays.equals(knownKey.getEncoded(), serverKey.getEncoded())) {
                    minecraft.execute(() -> {
                        Screen screen = WrongServerKeyWarningScreen.create(this, knownKey, serverKey);
                        minecraft.setScreen(screen);
                    });
                } else {
                    sendServerChallenge();
                }
            }
            case S2CSignaturePayload serverSignaturePayload -> {
                Constants.LOG.info("GOT SERVER'S SIGNATURE: {}", Base64Util.encode(serverSignaturePayload.signature));

                if (serverKey == null) {
                    var err = "Got a challenge response signature from the server before its key could be known!";
                    Constants.LOG.error(err);
                    this.connection.disconnect(Component.literal(err));

                    return;
                }

                if (nonce == null) {
                    var err = "Got a bogus challenge response from the server!";
                    Constants.LOG.error(err);
                    this.connection.disconnect(Component.literal(err));

                    return;
                }

                if (!serverSignaturePayload.verify(serverKey, nonce)) {
                    Constants.LOG.warn("Failed to verify signature from server!");
                    this.connection.disconnect(Component.translatable("authorisedkeysmc.error.server-auth-fail"));

                    return;
                }

                sendPublicKeyForAuthentication();
            }
            case S2CChallengePayload challengePayload -> {
                Constants.LOG.info("SERVER WANTS US TO SIGN THIS: {}", Base64Util.encode(challengePayload.getNonce()));

                this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, C2SSignaturePayload.fromSigningChallenge(secretKey, challengePayload)));
                this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.waiting-verdict"));
            }
            case S2CKeyRejectedPayload ignored -> {
                try {
                    secretKey = AuthorisedKeysModClient.KEY_PAIRS.getDefaultKey();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Ed25519PublicKeyParameters pub = secretKey.generatePublicKey();
                Constants.LOG.info("KEY REJECTED! Sending pubkey: {}", Base64Util.encode(pub.getEncoded()));
                this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, new C2SPublicKeyPayload(pub)));
                this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.key-rejected"));
            }
            case S2CRegistrationRequestPayload ignored -> {
                registering = true;

                Constants.LOG.info("SERVER WANTS US TO REGISTER!");

                minecraft.execute(() -> {
                    LoginRegistrationScreen screen = new LoginRegistrationScreen(this);
                    minecraft.setScreen(screen);
                });
            }
            default -> throw new IllegalArgumentException("Unknown base query payload type of %s!".formatted(payload.getClass().getName()));
        }
    }

    public void sendServerChallenge() {
        C2SChallengePayload challengePayload = new C2SChallengePayload();
        nonce = challengePayload.getNonce();

        this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, challengePayload));
        this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.verifying"));
    }

    public void sendPublicKeyForAuthentication() {
        try {
            secretKey = AuthorisedKeysModClient.KEY_PAIRS.getDefaultKey();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Ed25519PublicKeyParameters pub = secretKey.generatePublicKey();
        Constants.LOG.info("Sending pubkey for authentication: {}", Base64Util.encode(pub.getEncoded()));
        this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, new C2SPublicKeyPayload(pub)));

        this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.waiting-for-challenge"));
    }

    public void confirmRegistration() {
        try {
            secretKey = AuthorisedKeysModClient.KEY_PAIRS.getDefaultKey();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Ed25519PublicKeyParameters pub = secretKey.generatePublicKey();
        Constants.LOG.info("Proceeding with registration! Sending pubkey: {}", Base64Util.encode(pub.getEncoded()));
        connection.send(new ServerboundCustomQueryAnswerPacket(txId, new C2SPublicKeyPayload(pub)));
        updateStatus.accept(Component.translatable("authorisedkeysmc.status.registering"));
    }

    public void refuseRegistration() {
        Constants.LOG.info("Refusing to register!");

        connection.send(new ServerboundCustomQueryAnswerPacket(txId, new C2SRefuseRegistrationPayload()));
        updateStatus.accept(Component.translatable("authorisedkeysmc.status.refusing-to-register"));
    }

    public void cancelLogin() {
        Constants.LOG.info("Cancelled log-in!");
        connection.disconnect(Component.translatable("connect.aborted"));
    }
}
