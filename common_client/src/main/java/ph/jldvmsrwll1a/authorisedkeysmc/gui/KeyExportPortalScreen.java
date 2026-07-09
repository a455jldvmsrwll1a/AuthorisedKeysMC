package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public class KeyExportPortalScreen extends BaseScreen {
    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.export-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component EXPORT_CODE_LABEL =
            Component.translatable("authorisedkeysmc.button.key-export-to-code");
    private static final Component BROWSE_LABEL = Component.translatable("authorisedkeysmc.button.browse-keys-folder");

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int STRIDE = BUTTON_HEIGHT + 4;

    private final Screen parent;
    private final AkKeyPair keypair;

    public KeyExportPortalScreen(Screen parent, AkKeyPair keypair) {
        super(TITLE_LABEL);

        this.parent = parent;
        this.keypair = keypair;
    }

    @Override
    public void init() {
        int yCentre = height / 2 - BUTTON_HEIGHT / 2;

        StringWidget header = new StringWidget(TITLE_LABEL, font);
        header.setPosition(width / 2 - header.getWidth() / 2, 40);

        addRenderableWidget(header);
        addRenderableWidget(Button.builder(
                        EXPORT_CODE_LABEL, button -> onClose())
                .bounds(width / 2 - BUTTON_WIDTH / 2, yCentre, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(
                Button.builder(BROWSE_LABEL, button -> Util.getPlatform().openPath(AkmcClient.FILE_PATHS.KEY_PAIRS_DIR))
                        .bounds(width / 2 - BUTTON_WIDTH / 2, yCentre + STRIDE, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(width / 2 - BUTTON_WIDTH / 2, height - BUTTON_HEIGHT - 20, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
