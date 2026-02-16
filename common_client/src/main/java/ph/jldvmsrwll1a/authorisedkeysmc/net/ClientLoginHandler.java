package ph.jldvmsrwll1a.authorisedkeysmc.net;

import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.*;
import ph.jldvmsrwll1a.authorisedkeysmc.mixin.ClientHandshakePacketListenerAccessorMixin;
import ph.jldvmsrwll1a.authorisedkeysmc.mixin.ConnectionAccessorMixin;
import ph.jldvmsrwll1a.authorisedkeysmc.net.payload.*;

public final class ClientLoginHandler {
    private static final int KEEPALIVE_INTERVAL_TICKS = 500;
    private final Minecraft minecraft;
    private final Screen originalScreen;
    private final Connection connection;
    private final EventLoop nettyLoop;
    private final Consumer<Component> updateStatus;
    private final @Nullable ServerData serverData;
    private final boolean usingVanillaAuthentication;

    private byte @Nullable [] sessionHash;
    private @Nullable AkPublicKey hostKey;
    private byte @Nullable [] c2sNonce;
    private @Nullable S2CChallengePayload s2cChallenge;
    private @Nullable AkKeyPair keypair;
    private Phase phase = Phase.HELLO;
    private int txId = -1;
    private int ticksUntilPing = KEEPALIVE_INTERVAL_TICKS;
    private boolean keyWasSelectedManually = false;

    public ClientLoginHandler(
            Minecraft minecraft,
            ClientHandshakePacketListenerImpl listener,
            boolean didVanillaAuthentication,
            Connection connection,
            Consumer<Component> updateStatus) {
        this.minecraft = minecraft;
        this.originalScreen = minecraft.screen;
        this.connection = connection;
        this.nettyLoop =
                ((ConnectionAccessorMixin) connection).getNettyChannel().eventLoop();
        this.updateStatus = updateStatus;
        this.serverData = ((ClientHandshakePacketListenerAccessorMixin) listener).getServerData();
        this.usingVanillaAuthentication = didVanillaAuthentication;

        AkmcClient.setLoginHandler(this);
    }

    public boolean disconnected() {
        return !connection.isConnected();
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }

    public Optional<String> getServerName() {
        return serverData != null ? Optional.of(serverData.name) : Optional.empty();
    }

    public Optional<String> getHostAddress() {
        return serverData != null ? Optional.of(serverData.ip) : Optional.empty();
    }

    public void setSessionHash(byte @NotNull [] hash) {
        Validate.isTrue(nettyLoop.inEventLoop(), "Setting session hash in the wrong thread.");
        Validate.validState(sessionHash == null, "Session hash was set twice.");

        sessionHash = hash;
    }

    public void handleLoginFinished() {
        getServerName().ifPresent(serverName -> {
            if (keyWasSelectedManually && keypair != null) {
                Constants.LOG.info(
                        "AKMC: Assigning the \"{}\" key pair to server \"{}\".", keypair.getName(), serverName);
                AkmcClient.KEY_USES.setKeyNameUsedForServer(serverName, keypair.getName());
            }
        });

        resetScreen();
        AkmcClient.clearLoginHandler();

        Constants.LOG.info("AKMC: log-in finished successfully.");
    }

    public void handleDisconnection() {
        resetScreen();
        AkmcClient.clearLoginHandler();
    }

    public void tick() {
        ticksUntilPing--;

        if (ticksUntilPing <= 0) {
            ticksUntilPing = KEEPALIVE_INTERVAL_TICKS;

            respond(new C2SPingPayload());
        }
    }

    public void handleRawMessage(CustomQueryPayload payload, int txId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        payload.write(buf);
        handleRawMessage(buf, txId);
    }

    public void handleRawMessage(FriendlyByteBuf buf, int txId) {
        this.txId = txId;

        QueryPayloadType kind = BaseS2CPayload.peekPayloadType(buf);
        switch (kind) {
            case PONG -> handlePong(new S2CPongPayload(buf));
            case SERVER_KEY -> handleServerKey(new S2CPublicKeyPayload(buf));
            case CLIENT_CHALLENGE_RESPONSE -> handleServerSignature(new S2CSignaturePayload(buf));
            case SERVER_CHALLENGE -> handleChallenge(new S2CChallengePayload(buf));
            case AUTHENTICATION_REQUEST -> handleAuthenticationRequest(new S2CAuthenticationRequestPayload(buf));
            case REGISTRATION_REQUEST -> handleRegistrationRequest(new S2CRegistrationRequestPayload(buf));
        }
    }

    private void handlePong(S2CPongPayload payload) {
        // Do nothing.
    }

    private void handleServerKey(S2CPublicKeyPayload payload) {
        Validate.validState(phase == Phase.HELLO, "Received unexpected server key.");

        hostKey = payload.key;

        AkPublicKey knownKey = getHostAddress()
                .map(address -> AkmcClient.KNOWN_HOSTS.getHostKey(address))
                .orElse(null);

        if (knownKey == null) {
            showScreen(UnknownServerKeyWarningScreen.create(this, payload.key, this::onHostKeyAction));
        } else if (!knownKey.equals(payload.key)) {
            showScreen(WrongServerKeyWarningScreen.create(this, knownKey, payload.key, this::onHostKeyAction));
        } else {
            acceptKeyAndSendChallenge();
        }
    }

    private void handleServerSignature(S2CSignaturePayload payload) {
        Validate.validState(phase == Phase.VERIFY_SERVER, "Received unexpected server signature.");

        if (hostKey == null) {
            var err = "Got a challenge response signature from the server before its key could be known!";
            Constants.LOG.error(err);
            connection.disconnect(Component.literal(err));

            return;
        }

        if (c2sNonce == null) {
            var err = "Got a bogus challenge response from the server!";
            Constants.LOG.error(err);
            connection.disconnect(Component.literal(err));

            return;
        }

        if (!payload.verify(hostKey, c2sNonce, sessionHash)) {
            Constants.LOG.warn("Failed to verify signature from server!");
            connection.disconnect(Component.translatable("authorisedkeysmc.error.server-auth-fail"));

            return;
        }

        Constants.LOG.info("Successfully verified the server's identity.");

        respond(new C2SIdAckPayload());
        transition(Phase.AWAIT_REQUEST);
    }

    private void handleAuthenticationRequest(S2CAuthenticationRequestPayload payload) {
        Validate.validState(connection.isEncrypted(), "Encryption must be enabled.");
        Validate.validState(phase == Phase.AWAIT_REQUEST, "Received unexpected authentication request.");

        if (!loadSavedKeyPair()) {
            showScreen(new KeySelectionScreen(originalScreen, this::onAuthenticationKeySelected));

            return;
        }

        sendAuthenticationKey();
    }

    private void handleRegistrationRequest(S2CRegistrationRequestPayload payload) {
        Validate.validState(phase == Phase.AWAIT_REQUEST, "Received unexpected registration request.");

        Constants.LOG.info("registration required = {}", payload.registrationRequired());

        transition(Phase.AWAIT_REGISTRATION_DECISION);
        showScreen(LoginRegistrationScreen.create(
                originalScreen, usingVanillaAuthentication, this::onRegistrationAction, this::cancelLogin));
    }

    private void handleChallenge(S2CChallengePayload payload) {
        Validate.validState(
                phase == Phase.AWAIT_LOGIN_CHALLENGE || phase == Phase.AWAIT_REGISTRATION_CHALLENGE,
                "Received unexpected challenge.");
        Validate.validState(sessionHash != null, "Session hash is null. This is impossible as encryption is required.");
        Validate.notNull(keypair, "handleChallenge(): Missing key pair.");

        s2cChallenge = payload;

        if (!AkmcClient.CACHED_KEYS.decryptKeypair(keypair)) {
            showScreen(new PasswordPromptScreen(minecraft.screen, keypair, this::onPrivateKeyDecrypted));

            return;
        }

        sendChallengeResponse();
    }

    private void onHostKeyAction(boolean shouldContinue) {
        nettyLoop.execute(() -> {
            if (shouldContinue) {
                acceptKeyAndSendChallenge();
            } else {
                cancelLogin();
            }
        });
    }

    private void onRegistrationAction(boolean shouldRegister) {
        nettyLoop.execute(() -> {
            if (shouldRegister) {
                confirmRegistration();
            } else {
                refuseRegistration();
            }
        });
    }

    private void onPrivateKeyDecrypted(AkKeyPair decryptedKeypair) {
        nettyLoop.execute(() -> {
            byte[] sessionHash = this.sessionHash;

            Validate.validState(
                    phase == Phase.AWAIT_LOGIN_CHALLENGE || phase == Phase.AWAIT_REGISTRATION_CHALLENGE,
                    "Received unexpected challenge.");
            Validate.validState(
                    sessionHash != null, "Session hash is null. This is impossible as encryption is required.");
            Validate.validState(s2cChallenge != null, "Did not yet receive a challenge.");

            keypair = decryptedKeypair;

            if (decryptedKeypair.requiresDecryption()) {
                cancelLogin();
            } else {
                sendChallengeResponse();
            }
        });
    }

    private void onAuthenticationKeySelected(@Nullable String keyName) {
        nettyLoop.execute(() -> {
            keyWasSelectedManually = true;

            if (keyName == null) {
                cancelLogin();

                return;
            }

            loadKeyPair(keyName);
            sendAuthenticationKey();
        });
    }

    private void onRegistrationKeySelected(@Nullable String keyName) {
        nettyLoop.execute(() -> {
            keyWasSelectedManually = true;

            if (keyName == null) {
                cancelLogin();

                return;
            }

            loadKeyPair(keyName);
            sendRegistrationKey();
        });
    }

    private void sendAuthenticationKey() {
        Validate.validState(phase == Phase.AWAIT_REQUEST, "Should not send authentication key at this time.");
        Validate.notNull(keypair, "sendAuthenticationKey(): Missing key pair.");

        Constants.LOG.info(
                "AKMC: Presenting the \"{}\" public key for authentication: {}",
                keypair.getName(),
                keypair.getTextualPublic());

        respond(new C2SPublicKeyPayload(keypair.getPublic()));
        updateStatus.accept(Component.translatable("authorisedkeysmc.status.waiting-for-challenge"));

        transition(Phase.AWAIT_LOGIN_CHALLENGE);
    }

    private void sendRegistrationKey() {
        Validate.validState(
                phase == Phase.AWAIT_REGISTRATION_DECISION, "Should not send registration key at this time.");
        Validate.notNull(keypair, "sendRegistrationKey(): Missing keypair.");

        Constants.LOG.info("Proceeding with registration! Sending pubkey: {}", keypair.getTextualPublic());
        respond(new C2SPublicKeyPayload(keypair.getPublic()));
        updateStatus.accept(Component.translatable("authorisedkeysmc.status.registering"));

        transition(Phase.AWAIT_REGISTRATION_CHALLENGE);
    }

    private void sendChallengeResponse() {
        Validate.validState(
                phase == Phase.AWAIT_LOGIN_CHALLENGE || phase == Phase.AWAIT_REGISTRATION_CHALLENGE,
                "Received unexpected challenge.");
        Validate.validState(sessionHash != null, "Session hash is null. This is impossible as encryption is required.");
        Validate.validState(s2cChallenge != null, "Did not yet receive a challenge.");
        Validate.notNull(keypair, "Missing key pair.");
        Validate.validState(!keypair.requiresDecryption(), "Key pair has not been decrypted.");

        respond(C2SSignaturePayload.fromSigningChallenge(
                keypair.getDecryptedPrivate(), s2cChallenge, sessionHash, phase == Phase.AWAIT_REGISTRATION_CHALLENGE));

        updateStatus.accept(Component.translatable("authorisedkeysmc.status.waiting-verdict"));
        transition(Phase.AWAIT_VERDICT);
    }

    private void acceptKeyAndSendChallenge() {
        Validate.validState(phase == Phase.HELLO, "Tried to send bogus server challenge.");
        Validate.notNull(hostKey, "Trying to send server challenge before host key is known.");

        getHostAddress().ifPresent(address -> AkmcClient.KNOWN_HOSTS.setHostKey(address, hostKey));

        C2SChallengePayload challengePayload = new C2SChallengePayload();
        c2sNonce = challengePayload.getNonce();

        respond(challengePayload);
        transition(Phase.VERIFY_SERVER);
        updateStatus.accept(Component.translatable("authorisedkeysmc.status.verifying"));
    }

    private void confirmRegistration() {
        Validate.validState(
                phase == Phase.AWAIT_REGISTRATION_DECISION, "Confirming non-existent registration request.");

        showScreen(new KeySelectionScreen(originalScreen, this::onRegistrationKeySelected));
    }

    private void refuseRegistration() {
        Validate.validState(phase == Phase.AWAIT_REGISTRATION_DECISION, "Refusing non-existent registration request.");

        Constants.LOG.info("AKMC: Refusing to register!");

        respond(new C2SRefuseRegistrationPayload());
        updateStatus.accept(Component.translatable("authorisedkeysmc.status.refusing-to-register"));

        transition(Phase.AWAIT_VERDICT);
    }

    private void cancelLogin() {
        Constants.LOG.info("AKMC: Log-in canceled by user.");
        connection.disconnect(Component.translatable("connect.aborted"));

        showScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    private void resetScreen() {
        if (minecraft.screen != originalScreen) {
            showScreen(originalScreen);
        }
    }

    private boolean loadSavedKeyPair() {
        Optional<String> name = getServerName();
        if (name.isEmpty()) {
            return false;
        }

        String keyName = AkmcClient.KEY_USES.getKeyNameUsedForServer(name.get());
        if (keyName == null) {
            return false;
        }

        loadKeyPair(keyName);

        return true;
    }

    private void loadKeyPair(String keyName) {
        try {
            keypair = AkmcClient.KEY_PAIRS.loadFromFile(keyName);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the \"%s\" key pair.".formatted(keyName), e);
        }
    }

    private void showScreen(@Nullable Screen screen) {
        minecraft.executeBlocking(() -> minecraft.setScreen(screen));
    }

    private void transition(Phase next) {
        Constants.LOG.info("{} -> {}", phase, next);
        Validate.validState(nettyLoop.inEventLoop(), "Changing phase in the wrong thread!");

        phase = next;
    }

    private void respond(BaseC2SPayload payload) {
        connection.send(new ServerboundCustomQueryAnswerPacket(txId, payload));
    }

    private enum Phase {
        /// While waiting for server key packet.
        HELLO,
        /// While waiting for server's signature.
        VERIFY_SERVER,
        /// After acknowledging server key and waiting for login or registration.
        AWAIT_REQUEST,
        /// After receiving registration request and waiting for user input.
        AWAIT_REGISTRATION_DECISION,
        /// After sending our key for authentication.
        AWAIT_LOGIN_CHALLENGE,
        /// After sending our key for registration.
        AWAIT_REGISTRATION_CHALLENGE,
        /// After sending challenge response.
        AWAIT_VERDICT,
    }
}
