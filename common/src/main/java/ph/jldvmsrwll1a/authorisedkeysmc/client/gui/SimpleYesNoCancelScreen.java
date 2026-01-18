package ph.jldvmsrwll1a.authorisedkeysmc.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import ph.jldvmsrwll1a.authorisedkeysmc.net.client.ClientLoginHandler;

public abstract class SimpleYesNoCancelScreen extends BaseScreen {
    protected final ClientLoginHandler loginHandler;
    protected final Screen parent;
    protected final LinearLayout layout;
    protected final Component prompt;

    private MultiLineTextWidget promptWidget;
    private int lastWidth;

    public SimpleYesNoCancelScreen(ClientLoginHandler loginHandler, Component title, Component prompt) {
        super(title);

        this.loginHandler = loginHandler;
        this.parent = loginHandler.getMinecraft().screen;

        layout = LinearLayout.vertical().spacing(8);
        this.prompt = prompt;
        lastWidth = width;
    }

    @Override
    protected void init() {
        super.init();

        layout.defaultCellSetting().alignHorizontallyLeft();
        layout.addChild(new StringWidget(title, font));
        promptWidget = layout.addChild(new MultiLineTextWidget(prompt, font).setMaxWidth(width - 50).setMaxRows(15));

        LinearLayout buttonLayout = layout.addChild(LinearLayout.horizontal().spacing(4));
        buttonLayout.defaultCellSetting().paddingTop(16);

        final int WIDTH = 74;
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_YES, button -> onYesClicked()).width(WIDTH).build());
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_NO, button -> onNoClicked()).width(WIDTH).build());

        if (hasCancelButton()) {
            buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onCancelClicked()).width(WIDTH).build());
        }

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected final void repositionElements() {
        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    @Override
    public void tick() {
        if (loginHandler.disconnected()) {
            minecraft.setScreen(parent);
        }

        if (width != lastWidth) {
            lastWidth = width;
            promptWidget.setMaxWidth(width - 50);
        }
    }

    protected boolean hasCancelButton() {
        return false;
    }

    protected abstract void onYesClicked();

    protected abstract void onNoClicked();

    protected void onCancelClicked() {
        minecraft.setScreen(parent);
    }
}
