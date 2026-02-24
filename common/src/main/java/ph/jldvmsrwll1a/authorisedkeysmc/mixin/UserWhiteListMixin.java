package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;

// Based on https://github.com/NikitaCartes/EasyWhitelist

@Mixin(UserWhiteList.class)
public class UserWhiteListMixin {
    @Inject(
            method = "getKeyForUser(Lnet/minecraft/server/players/NameAndId;)Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true)
    private void useNameAsKey(NameAndId profile, CallbackInfoReturnable<String> cir) {
        if (AkmcCore.CONFIG.matchPlayerListByName) {
            cir.setReturnValue(profile.name());
        }
    }
}
