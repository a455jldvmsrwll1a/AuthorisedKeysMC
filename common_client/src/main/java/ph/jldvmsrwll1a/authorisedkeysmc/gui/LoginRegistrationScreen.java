package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.function.BooleanConsumer;
import org.jspecify.annotations.Nullable;

public final class LoginRegistrationScreen extends BaseScreen {
    private static final Component TITLE = Component.translatable("authorisedkeysmc.screen.registration.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.GREEN);
    private static final Component PREAMBLE_LABEL =
            Component.translatable("authorisedkeysmc.screen.registration.preamble");
    private static final Component OFFLINE_WARN_LABEL = Component.translatable(
                    "authorisedkeysmc.screen.registration.offline-warn")
            .withStyle(ChatFormatting.GOLD);
    private static final Component PROMPT_LABEL = Component.translatable("authorisedkeysmc.screen.registration.prompt");
    private static final Component MANDATORY_LABEL = Component.translatable(
                    "authorisedkeysmc.screen.registration.mandatory")
            .withStyle(ChatFormatting.GOLD);
    private static final Tooltip MANDATORY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.screen.registration.mandatory"));

    private final Screen parent;
    private final LinearLayout layout;
    private final Component prompt;
    private final boolean registrationRequired;
    private final @Nullable BooleanConsumer actionCallback;
    private final @Nullable Runnable cancelCallback;

    private MultiLineTextWidget promptWidget;

    public LoginRegistrationScreen(
            Screen parent,
            boolean usingVanillaAuthentication,
            boolean registrationRequired,
            @Nullable BooleanConsumer onAction,
            @Nullable Runnable onCancel) {
        super(TITLE);

        this.registrationRequired = registrationRequired;

        MutableComponent prompt = PREAMBLE_LABEL.copy();

        if (!usingVanillaAuthentication) {
            prompt.append(OFFLINE_WARN_LABEL);
        }

        prompt.append(PROMPT_LABEL);

        if (registrationRequired) {
            prompt.append("\n\n");
            prompt.append(MANDATORY_LABEL);
        }

        this.prompt = prompt;

        this.parent = parent;
        actionCallback = onAction;
        cancelCallback = onCancel;

        layout = LinearLayout.vertical().spacing(8);
    }

    @Override
    protected void init() {
        super.init();

        layout.defaultCellSetting().alignHorizontallyLeft();
        layout.addChild(new StringWidget(title, font));
        promptWidget = layout.addChild(
                new MultiLineTextWidget(prompt, font).setMaxWidth(width - 50).setMaxRows(15));

        LinearLayout buttonLayout = layout.addChild(LinearLayout.horizontal().spacing(4));
        buttonLayout.defaultCellSetting().paddingTop(16);

        final int WIDTH = 74;
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_YES, button -> onYesClicked())
                .width(WIDTH)
                .build());

        Button noBtn = buttonLayout.addChild(Button.builder(CommonComponents.GUI_NO, button -> onNoClicked())
                .width(WIDTH)
                .build());

        if (registrationRequired) {
            noBtn.setTooltip(MANDATORY_TOOLTIP);
            noBtn.active = false;
        }

        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onCancelClicked())
                .width(WIDTH)
                .tooltip(Tooltip.create(CommonComponents.GUI_DISCONNECT))
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        promptWidget.setMaxWidth(Math.min(width - 50, 400));

        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    private void onYesClicked() {
        if (actionCallback != null) {
            actionCallback.accept(true);
        }
    }

    private void onNoClicked() {
        if (actionCallback != null) {
            actionCallback.accept(false);
        }
    }

    private void onCancelClicked() {
        if (cancelCallback != null) {
            cancelCallback.run();
        } else {
            minecraft.setScreen(parent);
        }
    }
}
