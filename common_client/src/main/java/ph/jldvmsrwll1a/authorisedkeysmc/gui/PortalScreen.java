package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public class PortalScreen extends BaseScreen {
    private static final SystemToast.SystemToastId RELOADED_TOAST = new SystemToast.SystemToastId(2000);

    private static final Component HEADER_LABEL =
            Component.literal(Constants.MOD_NAME).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    private static final Component KEYS_LABEL = Component.translatable("authorisedkeysmc.button.key-manager");
    private static final Component SERVERS_LABEL = Component.translatable("authorisedkeysmc.button.server-manager");
    private static final Component CONFIG_LABEL = Component.translatable("authorisedkeysmc.button.configuration");
    private static final Component RELOAD_LABEL = Component.translatable("authorisedkeysmc.button.reload");
    private static final Component RELOADED_LABEL = Component.translatable("authorisedkeysmc.toast.mod-reloaded");
    private static final Tooltip RELOAD_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.reload-mod"));

    private static final int BUTTON_WIDTH = 160;
    private static final int SHORT_BUTTON_WIDTH = BUTTON_WIDTH / 2 - 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int STRIDE = BUTTON_HEIGHT + 4;

    private final Screen parent;

    public PortalScreen(Screen parent) {
        super(HEADER_LABEL);

        this.parent = parent;
    }

    @Override
    public void init() {
        int yCentre = height / 2 - BUTTON_HEIGHT / 2;

        StringWidget header = new StringWidget(HEADER_LABEL, font);
        header.setPosition(width / 2 - header.getWidth() / 2, 40);

        addRenderableWidget(header);
        addRenderableWidget(Button.builder(KEYS_LABEL, button -> minecraft.setScreen(new KeyManagementScreen(this)))
                .bounds(width / 2 - BUTTON_WIDTH / 2, yCentre - STRIDE, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(
                Button.builder(SERVERS_LABEL, button -> minecraft.setScreen(new ServerManagementScreen(this)))
                        .bounds(width / 2 - BUTTON_WIDTH / 2, yCentre, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build());
        Button configBtn = addRenderableWidget(Button.builder(CONFIG_LABEL, button -> {})
                .tooltip(Tooltip.create(Component.literal("Work in progress!")))
                .bounds(width / 2 + 2, yCentre + STRIDE, SHORT_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(RELOAD_LABEL, this::onReloadButtonPressed)
                .tooltip(RELOAD_TOOLTIP)
                .bounds(width / 2 - 80, yCentre + STRIDE, SHORT_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - BUTTON_WIDTH / 2, height - BUTTON_HEIGHT - 20, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        configBtn.active = false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void onReloadButtonPressed(Button button) {
        AkmcClient.readFiles();

        SystemToast.addOrUpdate(minecraft.getToastManager(), RELOADED_TOAST, RELOADED_LABEL, null);
    }
}
