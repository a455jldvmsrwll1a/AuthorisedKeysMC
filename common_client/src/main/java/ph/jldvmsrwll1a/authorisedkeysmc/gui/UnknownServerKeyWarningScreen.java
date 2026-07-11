package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.*;
import org.apache.commons.lang3.function.BooleanConsumer;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.net.ClientLoginHandler;

public final class UnknownServerKeyWarningScreen extends BaseScreen {
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
    private static final Tooltip COPY_TOOLTIP = Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.copy"));

    private final LinearLayout layout;
    private final String serverName;
    private final String serverKey;

    private MultiLineTextWidget promptWidget;
    private MultiLineTextWidget promptWidget2;
    private Button serverKeyButton;

    private final @Nullable BooleanConsumer actionCallback;

    public UnknownServerKeyWarningScreen(
            ClientLoginHandler context, AkPublicKey serverKey, @Nullable BooleanConsumer action) {
        super(TITLE);

        actionCallback = action;
        serverName = context.getServerName().orElse("<no name>");
        this.serverKey = serverKey.toString();

        layout = LinearLayout.vertical().spacing(8);
    }

    @Override
    protected void init() {
        super.init();

        MutableComponent promptA = PROMPT1.copy();
        promptA.append(Component.literal(serverName).withStyle(ChatFormatting.GRAY));
        promptA.append(PROMPT2);

        MutableComponent promptB = Component.empty();
        promptB.append(PROMPT3);
        promptB.append("\n\n");
        promptB.append(PROMPT4);

        layout.defaultCellSetting().alignHorizontallyLeft();
        layout.addChild(new StringWidget(title, font));
        promptWidget = layout.addChild(
                new MultiLineTextWidget(promptA, font).setMaxWidth(width - 50).setMaxRows(15));
        serverKeyButton = layout.addChild(
                Button.builder(Component.literal(serverKey).withStyle(ChatFormatting.AQUA), button -> copyServerKey())
                        .width(280)
                        .tooltip(COPY_TOOLTIP)
                        .build());
        promptWidget2 = layout.addChild(
                new MultiLineTextWidget(promptB, font).setMaxWidth(width - 50).setMaxRows(15));

        LinearLayout buttonLayout = layout.addChild(LinearLayout.horizontal().spacing(4));
        buttonLayout.defaultCellSetting().paddingTop(16);

        final int WIDTH = 74;
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CONTINUE, button -> onContinueClicked())
                .width(WIDTH)
                .build());
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onCancelClicked())
                .width(WIDTH)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        int w = Math.clamp(width - 50, 100, 500);
        promptWidget.setMaxWidth(w);
        promptWidget2.setMaxWidth(w);

        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    private void copyServerKey() {
        minecraft.keyboardHandler.setClipboard(serverKey);
    }

    private void onContinueClicked() {
        if (actionCallback != null) {
            actionCallback.accept(true);
        }
    }

    private void onCancelClicked() {
        if (actionCallback != null) {
            actionCallback.accept(false);
        }
    }
}
