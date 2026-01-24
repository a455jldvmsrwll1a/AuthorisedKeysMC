package ph.jldvmsrwll1a.authorisedkeysmc.platform;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import ph.jldvmsrwll1a.authorisedkeysmc.net.VanillaLoginHandlerState;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getGameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
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
