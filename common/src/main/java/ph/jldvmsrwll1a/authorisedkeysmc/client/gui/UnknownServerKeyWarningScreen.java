package ph.jldvmsrwll1a.authorisedkeysmc.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.net.client.ClientLoginHandler;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public class UnknownServerKeyWarningScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.unknown-server-key.title").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GOLD);

    private final Ed25519PublicKeyParameters serverKey;

    public UnknownServerKeyWarningScreen(ClientLoginHandler loginHandler, Component prompt, Ed25519PublicKeyParameters serverKey) {
        super(loginHandler, TITLE, prompt);
        this.serverKey = serverKey;
    }

    public static UnknownServerKeyWarningScreen create(ClientLoginHandler loginHandler, Ed25519PublicKeyParameters serverKey) {
        String name = loginHandler.getServerName().orElse("<no name>");
        String keyStr = Base64Util.encode(serverKey.getEncoded());
        Component prompt = Component.translatable("authorisedkeysmc.screen.unknown-server-key.prompt", name, keyStr);

        return new UnknownServerKeyWarningScreen(loginHandler, prompt, serverKey);
    }

    @Override
    protected void onYesClicked() {
        minecraft.setScreen(parent);

        loginHandler.getServerName().ifPresent(name -> AuthorisedKeysModClient.KNOWN_SERVERS.setServerKey(name, serverKey));

        loginHandler.sendServerChallenge();
    }

    @Override
    protected void onNoClicked() {
        loginHandler.cancelLogin();
        minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }
}
