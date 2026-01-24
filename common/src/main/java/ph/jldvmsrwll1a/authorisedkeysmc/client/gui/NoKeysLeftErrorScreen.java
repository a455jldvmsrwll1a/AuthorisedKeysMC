package ph.jldvmsrwll1a.authorisedkeysmc.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class NoKeysLeftErrorScreen extends BaseScreen {
    private static final int MAX_WIDTH = 400;
    private static final int BACK_BTN_WIDTH = 74;

    private final LinearLayout layout;
    private final Component prompt;
    private final String[] keysUsed;

    private MultiLineTextWidget bodyWidget;
    private MultiLineTextWidget footerWidget;
    private ScrollableLayout scrollLayout;
    private Button backBtn;

    private final int scrollHeight = (font.lineHeight + 2) * 5 - font.lineHeight;

    private NoKeysLeftErrorScreen(String[] keysUsed, Component prompt) {
        super(Component.translatable("authorisedkeysmc.screen.no-keys-left.title")
                .withStyle(ChatFormatting.RED)
                .withStyle(ChatFormatting.BOLD));

        this.keysUsed = keysUsed;
        this.prompt = prompt;
        layout = LinearLayout.vertical().spacing(8);
    }

    public static NoKeysLeftErrorScreen create(String[] keysUsed) {
        return new NoKeysLeftErrorScreen(
                keysUsed, Component.translatable("authorisedkeysmc.screen.no-keys-left.body", keysUsed.length));
    }

    @Override
    protected void init() {
        super.init();

        int width = Math.min(MAX_WIDTH, this.width);

        layout.defaultCellSetting().alignVerticallyTop().alignHorizontallyLeft();
        layout.addChild(new StringWidget(title, font));
        bodyWidget = layout.addChild(
                new MultiLineTextWidget(prompt, font).setMaxWidth(width - 50).setMaxRows(15));

        LinearLayout listLayout = LinearLayout.vertical();
        listLayout.defaultCellSetting().alignHorizontallyLeft();
        listLayout.spacing(2);

        for (int i = 0; i < keysUsed.length; ++i) {
            ChatFormatting colour = i % 2 == 0 ? ChatFormatting.GRAY : ChatFormatting.WHITE;
            listLayout.addChild(new StringWidget(
                    Component.literal("%s. %s".formatted(i + 1, keysUsed[i])).withStyle(colour), font));
        }

        listLayout.arrangeElements();

        scrollLayout = new ScrollableLayout(Minecraft.getInstance(), listLayout, listLayout.getHeight());
        scrollLayout.setMaxHeight(Math.max(scrollHeight, height - 200));
        scrollLayout.setMinWidth(width - 50);
        layout.addChild(scrollLayout);

        footerWidget = layout.addChild(
                new MultiLineTextWidget(Component.translatable("authorisedkeysmc.screen.no-keys-left.footer"), font)
                        .setMaxWidth(width - 50)
                        .setMaxRows(15));

        LinearLayout buttonLayout = layout.addChild(LinearLayout.horizontal().spacing(4));
        buttonLayout.defaultCellSetting().paddingTop(16);

        buttonLayout.addChild(Button.builder(
                        Component.translatable("authorisedkeysmc.button.goto-server-config"),
                        button -> goToServerConfig())
                .width(74 * 2)
                .build());
        backBtn = buttonLayout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> goBack())
                .width(BACK_BTN_WIDTH)
                .build());

        repositionElements();

        backBtn.setX(layout.getRectangle().right() - BACK_BTN_WIDTH);

        layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    protected void repositionElements() {
        int width = Math.min(MAX_WIDTH, this.width);

        if (bodyWidget != null) {
            bodyWidget.setMaxWidth(width - 50);
        }

        if (footerWidget != null) {
            footerWidget.setMaxWidth(width - 50);
        }

        if (scrollLayout != null) {
            scrollLayout.setMaxHeight(Math.max(scrollHeight, height - 300));
            scrollLayout.setMinWidth(width - 50);
        }

        layout.arrangeElements();

        backBtn.setX(layout.getRectangle().right() - BACK_BTN_WIDTH);

        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    private void goBack() {
        Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    private void goToServerConfig() {
        Minecraft.getInstance().setScreen(new TitleScreen());
        Minecraft.getInstance()
                .getToastManager()
                .addToast(new SystemToast(
                        new SystemToast.SystemToastId(),
                        Component.literal("WORK IN PROGRESS"),
                        Component.literal("Not yet implemented. Sorry.")));
    }
}
