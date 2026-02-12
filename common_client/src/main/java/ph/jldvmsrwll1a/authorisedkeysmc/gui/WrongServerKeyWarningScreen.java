package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.BooleanConsumer;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public final class WrongServerKeyWarningScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.wrong-server-key.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.RED);

    public WrongServerKeyWarningScreen(Screen parent, Component prompt, BooleanConsumer action) {
        super(parent, TITLE, prompt, action, null);
    }

    public static WrongServerKeyWarningScreen create(
            ClientLoginHandler context,
            Ed25519PublicKeyParameters cachedKey,
            Ed25519PublicKeyParameters currentKey,
            BooleanConsumer action) {
        Screen parent = context.getMinecraft().screen;
        String name = context.getServerName().orElse("<no name>");
        String cachedKeyStr = Base64Util.encode(cachedKey.getEncoded());
        String currentKeyStr = Base64Util.encode(currentKey.getEncoded());
        Component prompt = Component.translatable(
                "authorisedkeysmc.screen.wrong-server-key.prompt", name, cachedKeyStr, currentKeyStr);

        return new WrongServerKeyWarningScreen(parent, prompt, action);
    }
}
