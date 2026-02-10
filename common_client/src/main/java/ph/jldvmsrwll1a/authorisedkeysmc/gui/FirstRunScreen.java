package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FirstRunScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 74;
    private static final int HORIZONTAL_SPACE = 60;
    private static final int MAX_WIDTH = 300;
    private static final int COOLDOWN_TICKS = 40;

    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.first-run.title").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component BODY_1_LABEL = Component.translatable("authorisedkeysmc.screen.first-run.body1");
    private static final Component BODY_2_LABEL = Component.translatable("authorisedkeysmc.screen.first-run.body2");
    private static final Component PROCEED_LABEL = Component.translatable("authorisedkeysmc.button.proceed");

    private final Screen parent;
    private final LinearLayout rootLayout;
    private MultiLineTextWidget bodyText;
    private Button proceedButton;
    private boolean onSecondPart = false;
    private int cooldown = COOLDOWN_TICKS;

    public FirstRunScreen(Screen parent) {
        super(TITLE_LABEL);

        this.parent = parent;
        rootLayout = LinearLayout.vertical().spacing(4);
    }

    @Override
    protected void init() {

        bodyText = new MultiLineTextWidget(BODY_1_LABEL, font);
        bodyText.setMaxWidth(elementWidth());

        proceedButton = Button.builder(PROCEED_LABEL, this::onProceedButtonPressed).width(BUTTON_WIDTH).build();
        proceedButton.active = false;

        rootLayout.addChild(new StringWidget(TITLE_LABEL, font));
        rootLayout.addChild(bodyText);
        rootLayout.addChild(SpacerElement.height(font.lineHeight));
        rootLayout.addChild(proceedButton);
        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        bodyText.setMaxWidth(elementWidth());

        rootLayout.arrangeElements();
        FrameLayout.centerInRectangle(rootLayout, getRectangle());
    }

    @Override
    public void tick() {
        super.tick();

        if (cooldown > 0) {
            proceedButton.active = false;
            cooldown--;
        } else {
            proceedButton.active = true;
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void onProceedButtonPressed(Button button) {
        cooldown = COOLDOWN_TICKS;

        if (onSecondPart) {
            minecraft.setScreen(new KeyCreationScreen(parent, name -> {}));
        } else {
            onSecondPart = true;
            bodyText.setMessage(BODY_2_LABEL);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }
}
