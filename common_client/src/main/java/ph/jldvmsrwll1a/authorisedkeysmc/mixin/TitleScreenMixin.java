package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.IconButton;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.PortalScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow
    private boolean fading;

    @Unique
    private IconButton authorisedKeysMC$button;

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "createNormalMenuOptions", at = @At("RETURN"))
    private void addModButton(int y, int spacingY, CallbackInfoReturnable<Integer> ci) {
        IconButton button = IconButton.builder(Constants.modId("widget/main_menu"), btn -> {
                    if (!AkmcClient.maybeShowFirstRunScreen(minecraft, new PortalScreen(minecraft.screen))) {
                        minecraft.setScreen(new PortalScreen(minecraft.screen));
                    }
                })
                .pos(width / 2 - 100 - spacingY, y)
                .size(20, 20)
                .padding(2)
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

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;fadeWidgets(F)V"))
    private void extractCurrentFade(TitleScreen instance, float fade, Operation<Void> original) {
        original.call(instance, fade);

        if (authorisedKeysMC$button != null) {
            authorisedKeysMC$button.setFade(fading ? fade : 1.0f);
        }
    }
}
