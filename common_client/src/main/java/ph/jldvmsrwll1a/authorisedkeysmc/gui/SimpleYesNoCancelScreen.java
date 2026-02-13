package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.BooleanConsumer;
import org.jspecify.annotations.Nullable;

public abstract class SimpleYesNoCancelScreen extends BaseScreen {
    protected final Screen parent;
    private final LinearLayout layout;
    private final Component prompt;

    private MultiLineTextWidget promptWidget;

    protected final @Nullable BooleanConsumer actionCallback;
    protected final @Nullable Runnable cancelCallback;

    public SimpleYesNoCancelScreen(
            Screen parent,
            Component title,
            Component prompt,
            @Nullable BooleanConsumer onAction,
            @Nullable Runnable onCancel) {
        super(title);

        this.parent = parent;
        actionCallback = onAction;
        cancelCallback = onCancel;

        layout = LinearLayout.vertical().spacing(8);
        this.prompt = prompt;
    }

    @Override
    protected final void init() {
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
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_NO, button -> onNoClicked())
                .width(WIDTH)
                .build());

        if (hasCancelButton()) {
            buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onCancelClicked())
                    .width(WIDTH)
                    .build());
        }

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected final void repositionElements() {
        promptWidget.setMaxWidth(Math.min(width - 50, 400));

        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    protected boolean hasCancelButton() {
        return false;
    }

    protected void onYesClicked() {
        if (actionCallback != null) {
            actionCallback.accept(true);
        }
    }

    protected void onNoClicked() {
        if (actionCallback != null) {
            actionCallback.accept(false);
        }
    }

    protected void onCancelClicked() {
        if (cancelCallback != null) {
            cancelCallback.run();
        } else {
            minecraft.setScreen(parent);
        }
    }
}
