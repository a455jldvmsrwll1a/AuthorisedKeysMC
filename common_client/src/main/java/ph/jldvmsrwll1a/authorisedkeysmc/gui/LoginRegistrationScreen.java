package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.BooleanConsumer;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.LoadedKeypair;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;

public final class LoginRegistrationScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.binding.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.GREEN);

    public LoginRegistrationScreen(Screen parent, Component prompt, BooleanConsumer action, Runnable onCancel) {
        super(parent, TITLE, prompt, action, onCancel);
    }

    public static LoginRegistrationScreen create(ClientLoginHandler loginHandler, BooleanConsumer action, Runnable onCancel) {
        if (loginHandler.getKeypair().isEmpty()) {
            throw new IllegalStateException("Login handler must already have a key pair.");
        }

        Screen parent = loginHandler.getMinecraft().screen;
        LoadedKeypair keypair = loginHandler.getKeypair().get();
        Component prompt = Component.translatable("authorisedkeysmc.screen.binding.prompt", keypair.getName());

        return new LoginRegistrationScreen(parent, prompt, action, onCancel);
    }

    @Override
    protected boolean hasCancelButton() {
        return true;
    }
}
