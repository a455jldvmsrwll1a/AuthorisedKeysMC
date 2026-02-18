package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.*;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;

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
    private static final MutableComponent KEY_MISSING_LABEL =
            Component.translatable("authorisedkeysmc.screen.server.key-missing");
    private static final MutableComponent KEY_UNREADABLE_LABEL =
            Component.translatable("authorisedkeysmc.screen.server.key-unreadable");
    private static final Component FORGET_LABEL =
            Component.translatable("authorisedkeysmc.button.forget").withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
    private static final Tooltip COPY_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.share-key"));
    private static final Tooltip UNKNOWN_HOST_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.unknown-host-key"));
    private static final Tooltip SELECT_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.select-key"));
    private static final Tooltip MUST_SELECT_KEY_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.must-select-key"));
    private static final Tooltip FORGET_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.forget"));

    private final Screen parent;
    private final LinearLayout rootLayout;
    private final Component hostKeyLabel;
    private final boolean hostKeyIsKnown;
    private final @Nullable String originalUsedKeyName;
    private final String serverName;
    private final String serverAddress;

    private @Nullable String usedKeyName;
    private @Nullable String usedKeyString;
    private boolean dirty = false;
    private boolean forgetHostKey = false;

    private MultiLineTextWidget preambleText;
    private Button hostKeyField;
    private Button usedKeyField;
    private Button hostKeyForgetBtn;
    private Button usedKeyForgetBtn;

    public ServerInfoScreen(Screen parent, ServerData server) {
        super(Component.translatable("authorisedkeysmc.screen.server.title", server.name)
                .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));

        rootLayout = LinearLayout.vertical().spacing(4);
        serverName = server.name;
        serverAddress = server.ip;

        this.parent = parent;

        AkPublicKey hostKey = AkmcClient.KNOWN_HOSTS.getHostKey(server.ip);
        hostKeyLabel = hostKey != null ? Component.literal(hostKey.toString()) : UNKNOWN_KEY_LABEL;
        hostKeyIsKnown = hostKey != null;

        AkmcClient.KEY_USES.read();
        originalUsedKeyName = AkmcClient.KEY_USES.getKeyNameUsedForServer(server.name);
    }

    @Override
    protected void init() {
        preambleText = new MultiLineTextWidget(PREAMBLE_LABEL, font);

        LinearLayout hostKeyRow = LinearLayout.horizontal().spacing(4);

        Tooltip hostKeyTooltip = hostKeyIsKnown ? COPY_KEY_TOOLTIP : UNKNOWN_HOST_KEY_TOOLTIP;
        hostKeyField = Button.builder(hostKeyLabel, this::onHostKeyFieldPressed)
                .tooltip(hostKeyTooltip)
                .width(elementWidth())
                .build();

        hostKeyRow.addChild(hostKeyField);
        hostKeyForgetBtn = hostKeyRow.addChild(Button.builder(FORGET_LABEL, this::onForgetHostKeyButtonPressed)
                .tooltip(FORGET_TOOLTIP)
                .width(20)
                .build());

        LinearLayout usedKeyRow = LinearLayout.horizontal().spacing(4);
        usedKeyRow.addChild(Button.builder(SELECT_LABEL, this::onSelectButtonPressed)
                .tooltip(SELECT_KEY_TOOLTIP)
                .width(20)
                .build());
        usedKeyField = usedKeyRow.addChild(Button.builder(NO_USED_KEY_LABEL, this::onUsedKeyNameFieldPressed)
                .width(elementWidth() - 48)
                .build());
        usedKeyForgetBtn = usedKeyRow.addChild(Button.builder(FORGET_LABEL, this::onForgetUsedKeyButtonPressed)
                .tooltip(FORGET_TOOLTIP)
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
        rootLayout.addChild(hostKeyRow);

        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(new StringWidget(USE_KEY_LABEL, font));
        rootLayout.addChild(usedKeyRow);

        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(buttonLayout);

        hostKeyField.active = hostKeyIsKnown;
        hostKeyForgetBtn.active = hostKeyIsKnown;

        setUsedKey(originalUsedKeyName);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        preambleText.setMaxWidth(elementWidth());
        hostKeyField.setWidth(elementWidth() - 24);
        usedKeyField.setWidth(elementWidth() - 48);

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
            AkmcClient.KEY_USES.setKeyNameUsedForServer(serverName, usedKeyName);
        }

        if (forgetHostKey) {
            AkmcClient.KNOWN_HOSTS.clearHostKey(serverAddress);
        }

        minecraft.setScreen(parent);
    }

    private void onSelectButtonPressed(Button button) {
        minecraft.setScreen(new KeySelectionScreen(this, this::onKeySelected));
    }

    private void onForgetHostKeyButtonPressed(Button button) {
        // a bit hacky
        forgetHostKey = true;
        hostKeyField.setMessage(UNKNOWN_KEY_LABEL);
        hostKeyField.setTooltip(UNKNOWN_HOST_KEY_TOOLTIP);
        hostKeyField.active = false;
        hostKeyForgetBtn.active = false;
    }

    private void onForgetUsedKeyButtonPressed(Button button) {
        setUsedKey(null);
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
        usedKeyString = null;

        Component usedKeyLabel = NO_USED_KEY_LABEL;

        if (usedKeyName != null) {
            try {
                AkKeyPair keyPair = AkmcClient.KEY_PAIRS.loadFromFile(usedKeyName);
                usedKeyLabel = Component.literal(keyPair.getName());
                usedKeyString = keyPair.getTextualPublic();
            } catch (NoSuchFileException e) {
                Constants.LOG.error("The \"{}\" key pair does not exist: {}", usedKeyName, e);

                usedKeyName = null;
                usedKeyField.active = false;
                usedKeyField.setMessage(Component.literal(keyName).append(" ").append(KEY_MISSING_LABEL));
                usedKeyField.setTooltip(Tooltip.create(Component.literal(e.getClass().getTypeName())));
                usedKeyForgetBtn.active = true;

                return;
            } catch (IllegalStateException | IOException e) {
                Constants.LOG.error("Could not load the \"{}\" key pair: {}", usedKeyName, e);

                usedKeyName = null;
                usedKeyField.active = false;
                usedKeyField.setMessage(Component.literal(keyName).append(" ").append(KEY_UNREADABLE_LABEL));
                usedKeyField.setTooltip(Tooltip.create(Component.literal(e.toString())));
                usedKeyForgetBtn.active = true;

                return;
            }
        }

        if (usedKeyField != null) {
            usedKeyField.active = usedKeyName != null;
            usedKeyField.setMessage(usedKeyLabel);
            usedKeyField.setTooltip(usedKeyString != null ? COPY_KEY_TOOLTIP : MUST_SELECT_KEY_TOOLTIP);
        }

        if (usedKeyForgetBtn != null) {
            usedKeyForgetBtn.active = usedKeyName != null;
        }
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }
}
