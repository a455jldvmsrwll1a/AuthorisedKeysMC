package ph.jldvmsrwll1a.authorisedkeysmc.client.gui;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import ph.jldvmsrwll1a.authorisedkeysmc.net.client.ClientLoginHandler;

public final class LoginRegistrationScreen extends BaseScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.registration.title");
    private static final Component PROMPT = Component.translatable("authorisedkeysmc.screen.registration.prompt");

    public LoginRegistrationScreen(ClientLoginHandler loginHandler) {
        super(loginHandler, TITLE, PROMPT);
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
