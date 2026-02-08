package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.PortalScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow
    private boolean fading;

    @Unique
    private int authorisedKeysMC$buttonX;

    @Unique
    private int authorisedKeysMC$buttonY;

    @Unique
    private int authorisedKeysMC$fadeColour;

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "createNormalMenuOptions", at = @At("RETURN"))
    private void addModButton(int y, int spacingY, CallbackInfoReturnable<Integer> ci) {
        authorisedKeysMC$buttonX = this.width / 2 - 100 - spacingY;
        authorisedKeysMC$buttonY = y;

        addRenderableWidget(Button.builder(Component.empty(), button -> {
                    minecraft.setScreen(new PortalScreen(minecraft.screen));
                })
                .bounds(authorisedKeysMC$buttonX, authorisedKeysMC$buttonY, 20, 20)
                .build());
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderIcon(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Identifier texture = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/main-menu.png");

        int colour = fading ? authorisedKeysMC$fadeColour : 0xFFFFFFFF;
        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                authorisedKeysMC$buttonX + 2,
                authorisedKeysMC$buttonY + 2,
                0,
                0,
                16,
                16,
                16,
                16,
                colour);
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
