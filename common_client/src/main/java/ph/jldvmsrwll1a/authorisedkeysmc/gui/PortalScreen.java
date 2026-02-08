package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public class PortalScreen extends BaseScreen {
    private static final Component HEADER_LABEL =
            Component.literal(Constants.MOD_NAME).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    private static final Component KEYS_LABEL = Component.translatable("authorisedkeysmc.button.key-manager");
    private static final Component SERVERS_LABEL = Component.translatable("authorisedkeysmc.button.server-manager");
    private static final Component CONFIG_LABEL = Component.translatable("authorisedkeysmc.button.configuration");

    private static final int BUTTON_WIDTH = 160;
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
        addRenderableWidget(Button.builder(SERVERS_LABEL, button -> {})
                .bounds(width / 2 - BUTTON_WIDTH / 2, yCentre, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(CONFIG_LABEL, button -> {})
                .bounds(width / 2 - BUTTON_WIDTH / 2, yCentre + STRIDE, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - BUTTON_WIDTH / 2, height - BUTTON_HEIGHT - 20, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
