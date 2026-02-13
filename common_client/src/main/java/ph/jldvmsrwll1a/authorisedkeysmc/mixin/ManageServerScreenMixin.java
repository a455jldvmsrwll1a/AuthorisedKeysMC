package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.ServerInfoScreen;

@Mixin(ManageServerScreen.class)
public abstract class ManageServerScreenMixin extends Screen {
    @Shadow
    @Final
    private ServerData serverData;

    @Unique
    private boolean authorisedKeysMC$shortcutEnabled = false;

    private ManageServerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addShortcutButton(CallbackInfo ci) {
        if (serverData.ip.isBlank() || serverData.name.isBlank()) {
            authorisedKeysMC$shortcutEnabled = false;

            return;
        }

        addRenderableWidget(
                Button.builder(Component.empty(), button -> minecraft.setScreen(new ServerInfoScreen(this, serverData)))
                        .bounds(width / 2 - 124, height / 4 + 72, 20, 20)
                        .build());

        authorisedKeysMC$shortcutEnabled = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderShortcutIcon(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!authorisedKeysMC$shortcutEnabled) {
            return;
        }

        Identifier texture = Constants.modId("textures/gui/main-menu.png");
        gui.blit(RenderPipelines.GUI_TEXTURED, texture, width / 2 - 122, height / 4 + 74, 0, 0, 16, 16, 16, 16);
    }
}
