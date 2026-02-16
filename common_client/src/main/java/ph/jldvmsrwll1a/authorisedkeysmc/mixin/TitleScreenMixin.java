package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.PortalScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow
    private boolean fading;

    @Unique
    private Button authorisedKeysMC$button;

    @Unique
    private int authorisedKeysMC$fadeColour;

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "createNormalMenuOptions", at = @At("RETURN"))
    private void addModButton(int y, int spacingY, CallbackInfoReturnable<Integer> ci) {
        Button button = Button.builder(Component.empty(), btn -> {
                    if (!AkmcClient.maybeShowFirstRunScreen(
                            minecraft, new PortalScreen(minecraft.screen))) {
                        minecraft.setScreen(new PortalScreen(minecraft.screen));
                    }
                })
                .bounds(this.width / 2 - 100 - spacingY, y, 20, 20)
                .build();

        authorisedKeysMC$button = addRenderableWidget(button);
    }

    /// Redirect "Multiplayer" button in the title screen.
    @WrapOperation(
            require = 0,
            method = "lambda$createNormalMenuOptions$10",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void redirectToFirstRunScreen(Minecraft instance, Screen guiScreen, Operation<Void> original) {
        if (!AkmcClient.maybeShowFirstRunScreen(instance, guiScreen)) {
            original.call(instance, guiScreen);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderIcon(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (authorisedKeysMC$button == null) {
            return;
        }

        final Identifier texture = Constants.modId("textures/gui/main-menu.png");
        int x = authorisedKeysMC$button.getX() + 2;
        int y = authorisedKeysMC$button.getY() + 2;
        int colour = fading ? authorisedKeysMC$fadeColour : 0xFFFFFFFF;

        gui.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 16, 16, 16, 16, colour);
    }

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;fadeWidgets(F)V"))
    private void extractCurrentFade(TitleScreen instance, float fade, Operation<Void> original) {
        // Convert alpha from [0, 1] to [0, 255].
        int alpha = (int) Math.round(fade * 255.0);
        // Shift alpha to MSB and OR with white.
        authorisedKeysMC$fadeColour = alpha << 24 | 0xFFFFFF;

        original.call(instance, fade);
    }
}
