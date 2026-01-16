package ph.jldvmsrwll1a.authorisedkeysmc.platform;

import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import ph.jldvmsrwll1a.authorisedkeysmc.net.VanillaLoginHandlerState;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public Path getGameDirectory() {
        return FMLLoader.getGamePath();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLLoader.getGamePath().resolve("config");
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