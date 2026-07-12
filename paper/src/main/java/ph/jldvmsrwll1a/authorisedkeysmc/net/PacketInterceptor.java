package ph.jldvmsrwll1a.authorisedkeysmc.net;

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.util.concurrent.ExecutorService;
import javax.crypto.SecretKey;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Authorisedkeysmc;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public final class PacketInterceptor extends ChannelDuplexHandler {

    private static final Field LISTENER_LOGIN_CHALLENGE_BYTES;
    private static final Field LISTENER_AUTHENTICATOR_POOL;
    private static final Method LISTENER_LOGIN_EVENT;

    private final Channel channel;
    private final NetworkProcessor networkProcessor;
    private final MinecraftServer server;
    private final Connection connection;

    private String username = null;
    private boolean hasClientEverResponded = false;
    private ServerLoginHandler.Sender mailbox = null;
    private byte[] sessionHash = null;

    public PacketInterceptor(Channel channel, NetworkProcessor networkProcessor) {
        this.channel = channel;
        this.networkProcessor = networkProcessor;
        this.server = MinecraftServer.getServer();
        this.connection = (Connection) channel.pipeline().get("packet_handler");
    }

    public void setMailbox(ServerLoginHandler.Sender mailbox) {
        this.mailbox = mailbox;
    }

    public byte[] getSessionHash() {
        return sessionHash;
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void stop() {
        networkProcessor.uninject(channel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (msg) {
            case ServerboundHelloPacket packet -> onC2SHello(ctx, packet);
            case ServerboundKeyPacket packet -> onC2SKey(ctx, packet);
            case ServerboundCustomQueryAnswerPacket packet -> onC2SCustom(ctx, packet);
            default -> super.channelRead(ctx, msg);
        }
    }

    private void onC2SHello(ChannelHandlerContext ctx, ServerboundHelloPacket packet) {
        username = packet.name();
        ServerLoginPacketListenerImpl login = (ServerLoginPacketListenerImpl) connection.getPacketListener();
        byte[] challenge = getLoginListenerChallengeBytes(login);
        byte[] pubkey = server.getKeyPair().getPublic().getEncoded();

        connection.send(new ClientboundHelloPacket("", pubkey, challenge, server.usesAuthentication()));
    }

    private void onC2SKey(ChannelHandlerContext ctx, ServerboundKeyPacket packet) {
        ServerLoginPacketListenerImpl login = (ServerLoginPacketListenerImpl) connection.getPacketListener();
        byte[] challenge = getLoginListenerChallengeBytes(login);
        PrivateKey privateKey = server.getKeyPair().getPrivate();
        SecretKey secretKey = null;
        try {
            secretKey = packet.getSecretKey(privateKey);
            sessionHash = Crypt.digestData("", this.server.getKeyPair().getPublic(), secretKey);
        } catch (CryptException e) {
            throw new RuntimeException(e);
        }

        if (!packet.isChallengeValid(challenge, privateKey)) {
            throw new IllegalStateException("Received invalid challenge from client.");
        }

        if (Authorisedkeysmc.PENDING_LOGINS.putIfAbsent(login, this) != null) {
            throw new IllegalStateException("Internal error: login data already exists!");
        }

        if (server.usesAuthentication()) {
            Constants.LOG.info("use authentication!!!!!!");
            // let original handler fire the auth thread
            try {
                super.channelRead(ctx, packet);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                connection.setEncryptionKey(secretKey);

                getLoginListenerAuthenticatorPool(login).execute(() -> {
                    assert login != null;

                    // start client verification (offline mode)
                    login.authenticatedProfile =
                            callLoginListenerPlayerPreLoginEvents(login, UUIDUtil.createOfflineProfile(username));
                    login.state = ServerLoginPacketListenerImpl.State.VERIFYING;
                });
            } catch (CryptException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onC2SCustom(ChannelHandlerContext ctx, ServerboundCustomQueryAnswerPacket packet) {
        if (mailbox == null) {
            // Custom authentication hasn't started yet. We are not expecting a response at this time.

            try {
                super.channelRead(ctx, packet);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return;
        }

        // Payload is null if client did not understand our custom query.
        if (packet.payload() == null) {
            if (!hasClientEverResponded) {
                // Did not understand the very first query we sent. Client most likely does not have the mod installed.

                String customMessage = AkmcCore.CONFIG.kickMessage;
                String message = customMessage != null ? customMessage : "Access denied!!! D:";

                connection.disconnect(Component.literal(message));
                Constants.LOG.info("{} does not have AKMC installed.", username);
            }

            try {
                super.channelRead(ctx, packet);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return;
        }

        hasClientEverResponded = true;
        mailbox.receive(packet.payload());
    }

    private static byte[] getLoginListenerChallengeBytes(ServerLoginPacketListenerImpl listener) {
        try {
            return (byte[]) LISTENER_LOGIN_CHALLENGE_BYTES.get(listener);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ExecutorService getLoginListenerAuthenticatorPool(ServerLoginPacketListenerImpl listener) {
        try {
            return (ExecutorService) LISTENER_AUTHENTICATOR_POOL.get(listener);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static GameProfile callLoginListenerPlayerPreLoginEvents(
            ServerLoginPacketListenerImpl listener, GameProfile profile) {
        try {
            return (GameProfile) LISTENER_LOGIN_EVENT.invoke(listener, profile);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            LISTENER_LOGIN_CHALLENGE_BYTES = ServerLoginPacketListenerImpl.class.getDeclaredField("challenge");
            LISTENER_LOGIN_CHALLENGE_BYTES.setAccessible(true);

            LISTENER_AUTHENTICATOR_POOL = ServerLoginPacketListenerImpl.class.getDeclaredField("authenticatorPool");
            LISTENER_AUTHENTICATOR_POOL.setAccessible(true);

            LISTENER_LOGIN_EVENT = ServerLoginPacketListenerImpl.class.getDeclaredMethod(
                    "callPlayerPreLoginEvents", GameProfile.class);
            LISTENER_LOGIN_EVENT.setAccessible(true);
        } catch (NoSuchFieldException | InaccessibleObjectException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
