package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import java.util.Map;
import net.fabricmc.fabric.impl.networking.server.ServerLoginNetworkAddon;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

@Mixin(ServerLoginNetworkAddon.class)
public abstract class LoginAddonMixin {
    @Shadow
    @Final
    private Map<Integer, Identifier> channels;

    @Inject(method = "queryTick", at = @At("HEAD"))
    private void removeExternalChannel(CallbackInfoReturnable<Boolean> cir) {
        // This is required to make the server proceed with verification.
        channels.entrySet().removeIf(entry -> entry.getValue() == Constants.LOGIN_CHANNEL_ID);
    }
}
