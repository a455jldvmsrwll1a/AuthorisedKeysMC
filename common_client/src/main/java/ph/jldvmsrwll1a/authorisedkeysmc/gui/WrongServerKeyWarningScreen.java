package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.BooleanConsumer;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;

public final class WrongServerKeyWarningScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.wrong-server-key.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.RED);

    public WrongServerKeyWarningScreen(Screen parent, Component prompt, BooleanConsumer action) {
        super(parent, TITLE, prompt, action, null);
    }

    public static WrongServerKeyWarningScreen create(
            ClientLoginHandler context, AkPublicKey cachedKey, AkPublicKey currentKey, BooleanConsumer action) {
        Screen parent = context.getMinecraft().screen;
        String name = context.getServerName().orElse("<no name>");
        Component prompt = Component.translatable(
                "authorisedkeysmc.screen.wrong-server-key.prompt", name, cachedKey.toString(), currentKey.toString());

        return new WrongServerKeyWarningScreen(parent, prompt, action);
    }
}
