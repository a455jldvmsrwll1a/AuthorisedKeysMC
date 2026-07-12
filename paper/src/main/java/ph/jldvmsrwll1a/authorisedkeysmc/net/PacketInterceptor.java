package ph.jldvmsrwll1a.authorisedkeysmc.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.security.PrivateKey;
import javax.crypto.SecretKey;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.CryptException;

public final class PacketInterceptor extends ChannelDuplexHandler {

    private static final Field FIELD_LOGIN_CHALLENGE_BYTES;

    private final Channel channel;
    private final NetworkProcessor networkProcessor;
    private final MinecraftServer server;
    private final Connection connection;

    private boolean active = true;
    private String username = null;

    public PacketInterceptor(Channel channel, NetworkProcessor networkProcessor) {
        this.channel = channel;
        this.networkProcessor = networkProcessor;
        this.server = MinecraftServer.getServer();
        this.connection = (Connection) channel.pipeline().get("packet_handler");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!active) {
            super.channelRead(ctx, msg);

            return;
        }

        switch (msg) {
            case ServerboundHelloPacket packet -> onC2SHello(ctx, packet);
            case ServerboundKeyPacket packet -> onC2SKey(ctx, packet);
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

        if (!packet.isChallengeValid(challenge, privateKey)) {
            throw new IllegalStateException("Received invalid challenge from client.");
        }

        if (server.usesAuthentication()) {
            // let original handler fire the auth thread
            try {
                super.channelRead(ctx, packet);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                SecretKey secretKey = packet.getSecretKey(privateKey);
                connection.setEncryptionKey(secretKey);
                networkProcessor.uninject(channel);

                // start client verification (offline mode)
                login.authenticatedProfile = UUIDUtil.createOfflineProfile(username);
                login.state = ServerLoginPacketListenerImpl.State.VERIFYING;
            } catch (CryptException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static byte[] getLoginListenerChallengeBytes(ServerLoginPacketListenerImpl listener) {
        try {
            return (byte[]) FIELD_LOGIN_CHALLENGE_BYTES.get(listener);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            FIELD_LOGIN_CHALLENGE_BYTES = ServerLoginPacketListenerImpl.class.getDeclaredField("challenge");
            FIELD_LOGIN_CHALLENGE_BYTES.setAccessible(true);
        } catch (NoSuchFieldException | InaccessibleObjectException e) {
            throw new RuntimeException(e);
        }
    }
}
