package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public class KeyAddScreen extends BaseScreen {
    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.add-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component GENERATE_LABEL = Component.translatable("authorisedkeysmc.button.add-key-generate");
    private static final Component IMPORT_CODE_LABEL =
            Component.translatable("authorisedkeysmc.button.add-key-import-code");
    private static final Component BROWSE_LABEL = Component.translatable("authorisedkeysmc.button.browse-keys-folder");

    private static final int BUTTON_WIDTH = 160;
    private static final int SHORT_BUTTON_WIDTH = BUTTON_WIDTH / 2 - 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int STRIDE = BUTTON_HEIGHT + 4;

    private final Screen parent;
    private final Consumer<Optional<? extends AkKeyPair>> callback;

    public KeyAddScreen(Screen parent, Consumer<Optional<? extends AkKeyPair>> callback) {
        super(TITLE_LABEL);

        this.parent = parent;
        this.callback = callback;
    }

    @Override
    public void init() {
        int yCentre = height / 2 - BUTTON_HEIGHT / 2;

        StringWidget header = new StringWidget(TITLE_LABEL, font);
        header.setPosition(width / 2 - header.getWidth() / 2, 40);

        addRenderableWidget(header);
        addRenderableWidget(
                Button.builder(GENERATE_LABEL, button -> minecraft.gui.setScreen(new KeyCreationScreen(parent, callback)))
                        .bounds(width / 2 - BUTTON_WIDTH / 2, yCentre - STRIDE, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build());
        addRenderableWidget(Button.builder(
                        IMPORT_CODE_LABEL, button -> minecraft.gui.setScreen(new KeyCreationScreen(parent, callback)))
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
