package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import org.apache.commons.lang3.function.BooleanConsumer;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;

public class UnknownServerKeyWarningScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.unknown-server-key.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.GOLD);
    private static final Component PROMPT1 =
            Component.translatable("authorisedkeysmc.screen.unknown-server-key.prompt1");
    private static final Component PROMPT2 =
            Component.translatable("authorisedkeysmc.screen.unknown-server-key.prompt2");
    private static final Component PROMPT3 = Component.translatable(
                    "authorisedkeysmc.screen.unknown-server-key.prompt3")
            .withStyle(ChatFormatting.GOLD);
    private static final Component PROMPT4 =
            Component.translatable("authorisedkeysmc.screen.unknown-server-key.prompt4");
    private static final Component COPY_LABEL = Component.translatable("authorisedkeysmc.tooltip.copy");

    public UnknownServerKeyWarningScreen(Screen parent, Component prompt, BooleanConsumer action) {
        super(parent, TITLE, prompt, action, null);
    }

    public static UnknownServerKeyWarningScreen create(
            ClientLoginHandler context, AkPublicKey serverKey, BooleanConsumer action) {
        Screen parent = context.getMinecraft().screen;
        String name = context.getServerName().orElse("<no name>");
        String keyStr = serverKey.toString();

        MutableComponent prompt = PROMPT1.copy();
        prompt.append(Component.literal(name).withStyle(ChatFormatting.GRAY));
        prompt.append(PROMPT2);
        prompt.append("   ");
        prompt.append(Component.literal(keyStr)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent.ShowText(COPY_LABEL))
                        .withClickEvent(new ClickEvent.CopyToClipboard(keyStr))));
        prompt.append("\n\n");
        prompt.append(PROMPT3);
        prompt.append("\n\n");
        prompt.append(PROMPT4);

        return new UnknownServerKeyWarningScreen(parent, prompt, action);
    }
}
