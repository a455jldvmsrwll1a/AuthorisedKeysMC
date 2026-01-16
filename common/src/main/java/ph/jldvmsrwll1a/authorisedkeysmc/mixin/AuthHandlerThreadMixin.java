package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import net.minecraft.server.notifications.ServerActivityMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

@Mixin(targets = "net.minecraft.server.network.ServerLoginPacketListenerImpl$1")
public abstract class AuthHandlerThreadMixin {
    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/notifications/ServerActivityMonitor;reportLoginActivity()V"))
    private void dontReportLoginActivity(ServerActivityMonitor monitor) {
        Constants.LOG.debug("No-op report login activity.");
        /* no-op */
    }
}
