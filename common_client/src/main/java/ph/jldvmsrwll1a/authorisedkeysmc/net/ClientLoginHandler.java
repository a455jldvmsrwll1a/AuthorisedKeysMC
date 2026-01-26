package ph.jldvmsrwll1a.authorisedkeysmc.net;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.*;
import java.util.function.Consumer;
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
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.LoginRegistrationScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.NoKeysLeftErrorScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.UnknownServerKeyWarningScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.WrongServerKeyWarningScreen;
import ph.jldvmsrwll1a.authorisedkeysmc.mixin.ClientHandshakePacketListenerAccessorMixin;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.*;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public final class ClientLoginHandler {
    private final Minecraft minecraft;
    private final ClientHandshakePacketListenerImpl listener;
    private final Connection connection;
    private final Consumer<Component> updateStatus;

    private byte @Nullable [] sessionHash;

    private Ed25519PublicKeyParameters serverKey;
    private byte[] nonce;

    private Ed25519PrivateKeyParameters secretKey;
    private String secretKeyName;
    private Queue<String> remainingSecretKeys;
    private String[] allSecretKeyNames;

    boolean registering;

    private volatile int txId = -1;

    public ClientLoginHandler(
            Minecraft minecraft,
            ClientHandshakePacketListenerImpl listener,
            Connection connection,
            Consumer<Component> updateStatus) {
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
        return Optional.ofNullable(((ClientHandshakePacketListenerAccessorMixin) listener).getServerData())
                .map(data -> data.name);
    }

    public Optional<String> getHostAddress() {
        return Optional.ofNullable(((ClientHandshakePacketListenerAccessorMixin) listener).getServerData())
                .map(data -> data.ip);
    }

    public void setSessionHash(byte @NotNull [] hash) {
        sessionHash = hash;
    }

    public void handleRawMessage(CustomQueryPayload payload, int txId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        handleRawMessage(buf, txId);
    }

    public void handleRawMessage(FriendlyByteBuf buf, int txId) {
        QueryPayloadType kind = BaseS2CPayload.peekPayloadType(buf);
        BaseS2CPayload base =
                switch (kind) {
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

                Ed25519PublicKeyParameters knownKey = getHostAddress()
                        .map(address -> AuthorisedKeysModClient.KNOWN_HOSTS.getHostKey(address))
                        .orElse(null);

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

                if (!serverSignaturePayload.verify(serverKey, nonce, sessionHash)) {
                    Constants.LOG.warn("Failed to verify signature from server!");
                    this.connection.disconnect(Component.translatable("authorisedkeysmc.error.server-auth-fail"));

                    return;
                }

                tryAcquireNextSecretKey();
                sendPublicKeyForAuthentication();
            }
            case S2CChallengePayload challengePayload -> {
                if (sessionHash == null) {
                    throw new IllegalStateException(
                            "Session hash is null. This is impossible as encryption is required.");
                }

                var c2s =
                        C2SSignaturePayload.fromSigningChallenge(secretKey, challengePayload, sessionHash, registering);
                this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, c2s));
                this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.waiting-verdict"));
            }
            case S2CKeyRejectedPayload ignored -> {
                tryAcquireNextSecretKey();

                Ed25519PublicKeyParameters pub = secretKey.generatePublicKey();
                this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, new C2SPublicKeyPayload(pub)));
                this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.key-rejected"));
            }
            case S2CRegistrationRequestPayload ignored -> {
                registering = true;

                minecraft.execute(() -> {
                    LoginRegistrationScreen screen =
                            LoginRegistrationScreen.create(this, secretKeyName, secretKey.generatePublicKey());
                    minecraft.setScreen(screen);
                });
            }
            default ->
                throw new IllegalArgumentException("Unknown base query payload type of %s!"
                        .formatted(payload.getClass().getName()));
        }
    }

    public void sendServerChallenge() {
        C2SChallengePayload challengePayload = new C2SChallengePayload();
        nonce = challengePayload.getNonce();

        this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, challengePayload));
        this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.verifying"));
    }

    public void sendPublicKeyForAuthentication() {
        if (!connection.isEncrypted()) {
            throw new IllegalStateException("Encryption must be enabled.");
        }

        Ed25519PublicKeyParameters pub = secretKey.generatePublicKey();
        Constants.LOG.info("Sending pubkey for authentication: {}", Base64Util.encode(pub.getEncoded()));
        this.connection.send(new ServerboundCustomQueryAnswerPacket(txId, new C2SPublicKeyPayload(pub)));

        this.updateStatus.accept(Component.translatable("authorisedkeysmc.status.waiting-for-challenge"));
    }

    public void confirmRegistration() {
        getHostAddress().ifPresent(name -> {
            AuthorisedKeysModClient.KNOWN_HOSTS.addKeyForHost(name, secretKeyName);
        });

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

    private void tryAcquireNextSecretKey() {
        if (acquireNextSecretKey()) {
            return;
        }

        Constants.LOG.warn("No keys available to authenticate.");
        connection.disconnect(Component.translatable("connect.aborted"));

        minecraft.execute(() -> {
            if (allSecretKeyNames == null) {
                allSecretKeyNames = new String[0];
            }
            NoKeysLeftErrorScreen screen = NoKeysLeftErrorScreen.create(allSecretKeyNames);
            minecraft.setScreen(screen);
        });
    }

    private boolean acquireNextSecretKey() {
        secretKey = null;
        secretKeyName = null;

        if (remainingSecretKeys == null) {
            List<String> keyNames = AuthorisedKeysModClient.KNOWN_HOSTS.getKeysUsedForHost(
                    getHostAddress().orElse(null));
            if (keyNames.isEmpty()) {
                return false;
            }

            remainingSecretKeys = new ArrayDeque<>(keyNames);
            allSecretKeyNames = keyNames.toArray(new String[0]);
        }

        while (secretKey == null) {
            secretKeyName = remainingSecretKeys.poll();
            if (secretKeyName == null) {
                // Queue was emptied.
                return false;
            }

            try {
                var tryKey = AuthorisedKeysModClient.KEY_PAIRS.privateKeyFromFile(secretKeyName);

                if (tryKey.isPresent()) {
                    secretKey = tryKey.get();
                } else {
                    Constants.LOG.error("{} requires a password to decrypt but a password prompt has not yet been implemented.", secretKeyName);
                }
            } catch (InvalidPathException | IOException e) {
                Constants.LOG.error("Could not load the \"{}\" key: {}", secretKeyName, e);
            }
        }

        Constants.LOG.info("Trying the \"{}\" key.", secretKeyName);
        return true;
    }
}
