package ph.jldvmsrwll1a.authorisedkeysmc.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.net.client.ClientLoginHandler;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public class WrongServerKeyWarningScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.wrong-server-key.title").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.RED);

    private final Ed25519PublicKeyParameters serverKey;

    public WrongServerKeyWarningScreen(ClientLoginHandler loginHandler, Component prompt, Ed25519PublicKeyParameters serverKey) {
        super(loginHandler, TITLE, prompt);
        this.serverKey = serverKey;
    }

    public static WrongServerKeyWarningScreen create(ClientLoginHandler loginHandler, Ed25519PublicKeyParameters cachedKey, Ed25519PublicKeyParameters currentKey) {
        String name = loginHandler.getServerName().orElse("<no name>");
        String cachedKeyStr = Base64Util.encode(cachedKey.getEncoded());
        String currentKeyStr = Base64Util.encode(currentKey.getEncoded());
        Component prompt = Component.translatable("authorisedkeysmc.screen.wrong-server-key.prompt", name, cachedKeyStr, currentKeyStr);

        return new WrongServerKeyWarningScreen(loginHandler, prompt, currentKey);
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
