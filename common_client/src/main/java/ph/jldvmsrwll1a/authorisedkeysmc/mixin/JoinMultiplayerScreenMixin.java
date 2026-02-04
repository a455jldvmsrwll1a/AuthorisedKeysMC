package ph.jldvmsrwll1a.authorisedkeysmc.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ph.jldvmsrwll1a.authorisedkeysmc.gui.ManagementScreen;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Unique
    private static final int authorisedKeysMC$WIDTH = 74;

    @Unique
    private Button authorisedKeysMC$keyButton;

    private JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void addConfigButton(CallbackInfo ci) {
        Validate.notNull(minecraft, "Minecraft must not be null");

        authorisedKeysMC$keyButton = Button.builder(
                        Component.translatable("authorisedkeysmc.button.key-manager"), button -> {
                            minecraft.setScreen(new ManagementScreen(this));
                        })
                .size(authorisedKeysMC$WIDTH, 20)
                .pos(width - authorisedKeysMC$WIDTH - 6, 6)
                .build();
        addRenderableWidget(authorisedKeysMC$keyButton);
    }

    @Inject(method = "repositionElements", at = @At("HEAD"))
    private void repositionConfigButton(CallbackInfo ci) {
        authorisedKeysMC$keyButton.setPosition(width - authorisedKeysMC$WIDTH - 6, 6);
    }
}
