package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.function.BooleanConsumer;

public final class LoginRegistrationScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.registration.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.GREEN);
    private static final Component PREAMBLE_LABEL = Component.translatable("authorisedkeysmc.screen.registration.preamble");
    private static final Component OFFLINE_WARN_LABEL = Component.translatable("authorisedkeysmc.screen.registration.offline-warn").withStyle(ChatFormatting.GOLD);
    private static final Component PROMPT_LABEL = Component.translatable("authorisedkeysmc.screen.registration.prompt");

    public LoginRegistrationScreen(Screen parent, Component prompt, BooleanConsumer action, Runnable onCancel) {
        super(parent, TITLE, prompt, action, onCancel);
    }

    public static LoginRegistrationScreen create(Screen parent, boolean usingVanillaAuthentication, BooleanConsumer action, Runnable onCancel) {
        MutableComponent prompt = PREAMBLE_LABEL.copy();

        if (!usingVanillaAuthentication) {
            prompt.append(OFFLINE_WARN_LABEL);
        }

        prompt.append(PROMPT_LABEL);

        return new LoginRegistrationScreen(parent, prompt, action, onCancel);
    }

    @Override
    protected boolean hasCancelButton() {
        return true;
    }
}
