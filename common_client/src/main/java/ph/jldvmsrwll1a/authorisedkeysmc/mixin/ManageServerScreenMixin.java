package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.IconButton;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.ServerInfoScreen;

@Mixin(ManageServerScreen.class)
public abstract class ManageServerScreenMixin extends Screen {
    @Shadow
    @Final
    private ServerData serverData;

    private ManageServerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addShortcutButton(CallbackInfo ci) {
        if (serverData.ip.isBlank() || serverData.name.isBlank()) {
            return;
        }

        addRenderableWidget(IconButton.builder(
                        Constants.modId("widget/main_menu"),
                        button -> minecraft.setScreen(new ServerInfoScreen(this, serverData)))
                .pos(width / 2 - 124, height / 4 + 72)
                .size(20, 20)
                .padding(2)
                .build());
    }
}
