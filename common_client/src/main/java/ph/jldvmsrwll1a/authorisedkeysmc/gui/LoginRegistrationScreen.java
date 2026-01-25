package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public final class LoginRegistrationScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.registration.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.GREEN);

    public LoginRegistrationScreen(ClientLoginHandler loginHandler, Component prompt) {
        super(loginHandler, TITLE, prompt);
    }

    public static LoginRegistrationScreen create(
            ClientLoginHandler loginHandler, String keyName, Ed25519PublicKeyParameters key) {
        Component prompt = Component.translatable(
                "authorisedkeysmc.screen.registration.prompt", keyName, Base64Util.encode(key.getEncoded()));

        return new LoginRegistrationScreen(loginHandler, prompt);
    }

    @Override
    protected void onYesClicked() {
        minecraft.setScreen(parent);
        loginHandler.confirmRegistration();
    }

    @Override
    protected void onNoClicked() {
        minecraft.setScreen(parent);
        loginHandler.refuseRegistration();
    }

    @Override
    protected void onCancelClicked() {
        loginHandler.cancelLogin();
        minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    @Override
    protected boolean hasCancelButton() {
        return true;
    }
}
