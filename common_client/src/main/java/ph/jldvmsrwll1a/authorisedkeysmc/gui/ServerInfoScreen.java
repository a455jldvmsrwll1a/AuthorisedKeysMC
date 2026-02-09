package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.io.IOException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.LoadedKeypair;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public final class ServerInfoScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 74;
    private static final int HORIZONTAL_SPACE = 60;
    private static final int MAX_WIDTH = 550;

    private static final Component PREAMBLE_LABEL = Component.translatable("authorisedkeysmc.screen.server.preamble");
    private static final Component SELECT_LABEL = Component.literal("...");
    private static final Component HOST_KEY_LABEL = Component.translatable("authorisedkeysmc.screen.server.host-key");
    private static final Component UNKNOWN_KEY_LABEL =
            Component.translatable("authorisedkeysmc.screen.server.unknown-key");
    private static final Component USE_KEY_LABEL = Component.translatable("authorisedkeysmc.screen.server.use-key");
    private static final Component NO_USED_KEY_LABEL =
            Component.translatable("authorisedkeysmc.screen.server.no-used-key");
    private static final Tooltip COPY_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.share-key"));
    private static final Tooltip UNKNOWN_HOST_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.unknown-host-key"));
    private static final Tooltip SELECT_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.select-key"));
    private static final Tooltip MUST_SELECT_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.must-select-key"));

    private final Screen parent;
    private final LinearLayout rootLayout;
    private final Component hostKeyLabel;
    private final boolean hostKeyIsKnown;
    private final @Nullable String originalUsedKeyName;
    private final String serverName;

    private @Nullable String usedKeyName;
    private Component usedKeyLabel = NO_USED_KEY_LABEL;
    private @Nullable String usedKeyString;
    private boolean dirty = false;

    private MultiLineTextWidget preambleText;
    private Button hostKeyField;
    private Button usedKeyField;

    public ServerInfoScreen(Screen parent, ServerData server) {
        super(Component.translatable("authorisedkeysmc.screen.server.title", server.name)
                .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));

        rootLayout = LinearLayout.vertical().spacing(4);
        serverName = server.name;

        this.parent = parent;

        Ed25519PublicKeyParameters hostKey = AuthorisedKeysModClient.KNOWN_HOSTS.getHostKey(server.ip);
        hostKeyLabel = hostKey != null ? Component.literal(Base64Util.encode(hostKey.getEncoded())) : UNKNOWN_KEY_LABEL;
        hostKeyIsKnown = hostKey != null;

        AuthorisedKeysModClient.KEY_USES.read();
        originalUsedKeyName = AuthorisedKeysModClient.KEY_USES.getKeyNameUsedForServer(server.name);
    }

    @Override
    protected void init() {
        preambleText = new MultiLineTextWidget(PREAMBLE_LABEL, font);

        Tooltip hostKeyTooltip = hostKeyIsKnown ? COPY_KEY_TOOLTIP : UNKNOWN_HOST_KEY_TOOLTIP;
        hostKeyField = Button.builder(hostKeyLabel, this::onHostKeyFieldPressed)
                .tooltip(hostKeyTooltip)
                .width(elementWidth())
                .build();
        hostKeyField.active = hostKeyIsKnown;

        LinearLayout keyLayout = LinearLayout.horizontal().spacing(4);

        usedKeyField = Button.builder(NO_USED_KEY_LABEL, this::onUsedKeyNameFieldPressed)
                .width(elementWidth() - 20 - 4)
                .build();
        usedKeyField.active = false;

        keyLayout.addChild(usedKeyField);
        keyLayout.addChild(Button.builder(SELECT_LABEL, this::onSelectButtonPressed)
                .tooltip(SELECT_KEY_TOOLTIP)
                .width(20)
                .build());

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.defaultCellSetting().paddingTop(16);

        buttonLayout.addChild(Button.builder(CommonComponents.GUI_DONE, this::onDoneButtonPressed)
                .width(BUTTON_WIDTH)
                .build());
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> minecraft.setScreen(parent))
                .width(BUTTON_WIDTH)
                .build());

        rootLayout.addChild(new StringWidget(getTitle(), font));
        rootLayout.addChild(preambleText);

        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(new StringWidget(HOST_KEY_LABEL, font));
        rootLayout.addChild(hostKeyField);

        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(new StringWidget(USE_KEY_LABEL, font));
        rootLayout.addChild(keyLayout);

        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(buttonLayout);

        setUsedKey(originalUsedKeyName);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        preambleText.setMaxWidth(elementWidth());
        hostKeyField.setWidth(elementWidth());
        usedKeyField.setWidth(elementWidth() - 20 - 4);

        rootLayout.arrangeElements();
        FrameLayout.centerInRectangle(rootLayout, getRectangle());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void onHostKeyFieldPressed(Button button) {
        minecraft.keyboardHandler.setClipboard(hostKeyLabel.getString());
    }

    private void onUsedKeyNameFieldPressed(Button button) {
        if (usedKeyString != null) {
            minecraft.keyboardHandler.setClipboard(usedKeyString);
        }
    }

    private void onDoneButtonPressed(Button button) {
        if (dirty) {
            AuthorisedKeysModClient.KEY_USES.setKeyNameUsedForServer(serverName, usedKeyName);
        }

        minecraft.setScreen(parent);
    }

    private void onSelectButtonPressed(Button button) {
        minecraft.setScreen(new KeySelectionScreen(this, this::onKeySelected));
    }

    private void onKeySelected(String keyName) {
        if (keyName == null) {
            return;
        }

        setUsedKey(keyName);
    }

    private void setUsedKey(@Nullable String keyName) {
        if (keyName == null || !keyName.equals(usedKeyName)) {
            dirty = true;
        }

        usedKeyName = keyName;
        usedKeyLabel = NO_USED_KEY_LABEL;
        usedKeyString = null;

        if (usedKeyName != null) {
            try {
                LoadedKeypair keyPair = AuthorisedKeysModClient.KEY_PAIRS.loadFromFile(usedKeyName);
                usedKeyLabel = Component.literal(keyPair.getName());
                usedKeyString = keyPair.getTextualPublic();
            } catch (IllegalStateException | IOException e) {
                Constants.LOG.error("Could not load the \"{}\" key pair: {}", usedKeyName, e);

                usedKeyName = null;
            }
        }

        if (usedKeyField != null) {
            usedKeyField.active = usedKeyName != null;
            usedKeyField.setMessage(usedKeyLabel);
            usedKeyField.setTooltip(usedKeyString != null ? COPY_KEY_TOOLTIP : MUST_SELECT_KEY_TOOLTIP);
        }
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }
}
