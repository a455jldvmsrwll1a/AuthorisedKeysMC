package ph.jldvmsrwll1a.authorisedkeysmc.platform;

import java.nio.file.Path;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import ph.jldvmsrwll1a.authorisedkeysmc.net.VanillaLoginHandlerState;

public final class PaperPlatformHelper implements IPlatformHelper {
    private final Plugin plugin;

    public PaperPlatformHelper(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlatformName() {
        return "Paper";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Bukkit.getPluginManager().isPluginEnabled(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        // not sure how to query
        return false;
    }

    @Override
    public Path getGameDirectory() {
        return plugin.getServer().getWorldContainer().toPath();
    }

    @Override
    public Path getConfigDirectory() {
        return plugin.getDataPath();
    }

    @Override
    public VanillaLoginHandlerState getLoginState(ServerLoginPacketListenerImpl listener) {
        return switch (listener.state) {
            case HELLO -> VanillaLoginHandlerState.STARTING;
            case KEY -> VanillaLoginHandlerState.ENCRYPTING;
            case AUTHENTICATING -> VanillaLoginHandlerState.AUTHENTICATING;
            case NEGOTIATING -> VanillaLoginHandlerState.IN_CUSTOM_PROCESS;
            case VERIFYING -> VanillaLoginHandlerState.CHECKING_CAN_JOIN;
            case WAITING_FOR_DUPE_DISCONNECT -> VanillaLoginHandlerState.AWAITING_DEDUPLICATION;
            case PROTOCOL_SWITCHING -> VanillaLoginHandlerState.SWITCHING_PROTOCOL;
            case ACCEPTED -> VanillaLoginHandlerState.DONE;
        };
    }

    @Override
    public void setLoginState(ServerLoginPacketListenerImpl listener, VanillaLoginHandlerState state) {
        listener.state = switch (state) {
            case STARTING -> ServerLoginPacketListenerImpl.State.HELLO;
            case ENCRYPTING -> ServerLoginPacketListenerImpl.State.KEY;
            case AUTHENTICATING -> ServerLoginPacketListenerImpl.State.AUTHENTICATING;
            case IN_CUSTOM_PROCESS -> ServerLoginPacketListenerImpl.State.NEGOTIATING;
            case CHECKING_CAN_JOIN -> ServerLoginPacketListenerImpl.State.VERIFYING;
            case AWAITING_DEDUPLICATION -> ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT;
            case SWITCHING_PROTOCOL -> ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING;
            case DONE -> ServerLoginPacketListenerImpl.State.ACCEPTED;
        };
    }
}
