package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import org.apache.commons.lang3.function.BooleanConsumer;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;

public final class WrongServerKeyWarningScreen extends SimpleYesNoCancelScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.wrong-server-key.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.DARK_RED);
    private static final Component PROMPT1 = Component.translatable("authorisedkeysmc.screen.wrong-server-key.prompt1");
    private static final Component PROMPT2 = Component.translatable("authorisedkeysmc.screen.wrong-server-key.prompt2");
    private static final Component PROMPT3 = Component.translatable("authorisedkeysmc.screen.wrong-server-key.prompt3");
    private static final Component PROMPT4 = Component.translatable("authorisedkeysmc.screen.wrong-server-key.prompt4");
    private static final Component PROMPT5 = Component.translatable("authorisedkeysmc.screen.wrong-server-key.prompt5")
            .withStyle(ChatFormatting.GOLD);
    private static final Component PROMPT6 = Component.translatable("authorisedkeysmc.screen.wrong-server-key.prompt6");
    private static final Component COPY_LABEL = Component.translatable("authorisedkeysmc.tooltip.copy");

    public WrongServerKeyWarningScreen(Screen parent, Component prompt, BooleanConsumer action) {
        super(parent, TITLE, prompt, action, null);
    }

    public static WrongServerKeyWarningScreen create(
            ClientLoginHandler context, AkPublicKey cachedKey, AkPublicKey currentKey, BooleanConsumer action) {
        Screen parent = context.getMinecraft().screen;

        String cachedStr = cachedKey.toString();
        String currentStr = currentKey.toString();
        String name = context.getServerName().orElse("<no name>");

        MutableComponent prompt = PROMPT1.copy();
        prompt.append(Component.literal(name).withStyle(ChatFormatting.GRAY));
        prompt.append(PROMPT2);
        prompt.append("   ");
        prompt.append(Component.literal(cachedStr)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent.ShowText(COPY_LABEL))
                        .withClickEvent(new ClickEvent.CopyToClipboard(cachedStr))));
        prompt.append(PROMPT3);
        prompt.append(Component.literal(currentStr)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.RED)
                        .withHoverEvent(new HoverEvent.ShowText(COPY_LABEL))
                        .withClickEvent(new ClickEvent.CopyToClipboard(currentStr))));
        prompt.append(PROMPT4);
        prompt.append(PROMPT5);
        prompt.append(PROMPT6);

        return new WrongServerKeyWarningScreen(parent, prompt, action);
    }
}
