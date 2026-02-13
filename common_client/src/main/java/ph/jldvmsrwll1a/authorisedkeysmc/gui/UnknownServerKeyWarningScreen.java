package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.BooleanConsumer;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public class UnknownServerKeyWarningScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.unknown-server-key.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.GOLD);

    public UnknownServerKeyWarningScreen(Screen parent, Component prompt, BooleanConsumer action) {
        super(parent, TITLE, prompt, action, null);
    }

    public static UnknownServerKeyWarningScreen create(
            ClientLoginHandler context, AkPublicKey serverKey, BooleanConsumer action) {
        Screen parent = context.getMinecraft().screen;
        String name = context.getServerName().orElse("<no name>");
        String keyStr = Base64Util.encode(serverKey.getEncoded());
        Component prompt = Component.translatable("authorisedkeysmc.screen.unknown-server-key.prompt", name, keyStr);

        return new UnknownServerKeyWarningScreen(parent, prompt, action);
    }
}
