package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.*;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public final class KeyManagementScreen extends BaseScreen {
    private static final float WIDTH_LEFT = 4.0f / 11.0f;
    private static final float WIDTH_RIGHT = 1.f - WIDTH_LEFT;
    private static final float MIN_TOTAL_WIDTH = 350.0f;
    private static final float MAX_TOTAL_WIDTH = 600.0f;
    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.config.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.AQUA);
    private static final Component EMPTY_LIST_LABEL =
            Component.translatable("authorisedkeysmc.screen.config.keys.empty-list-message");
    private static final SystemToast.SystemToastId KEY_COPIED_TOAST = new SystemToast.SystemToastId(2000);
    private static final SystemToast.SystemToastId WIP_TOAST = new SystemToast.SystemToastId(1500);

    private final Screen parent;
    private final HeaderAndFooterLayout rootLayout;
    private final GridLayout contentLayout;

    private final int scrollHeight = (font.lineHeight + 2) * 5 - font.lineHeight;

    private List<String> keyNames;

    private GenericStringSelectionList keySelectionList;
    private ScrollableLayout inspectorScroller;
    private MultiLineTextWidget inspectorText;
    private List<Button> listButtons;
    private List<Button> inspectorButtons;
    private Button deleteButton;
    private Button passwordButton;

    private @Nullable AkKeyPair currentKeypair;
    private @Nullable String selectedKeyName;

    private boolean needsLayout = true;

    public KeyManagementScreen(Screen parent) {
        super(Component.translatable("authorisedkeysmc.screen.config.title"));

        this.parent = parent;

        rootLayout = new HeaderAndFooterLayout(this);
        contentLayout = new GridLayout();
    }

    protected void init() {
        reloadKeys();

        GridLayout.RowHelper rowHelper =
                contentLayout.columnSpacing(16).rowSpacing(4).createRowHelper(2);
        rowHelper.defaultCellSetting().alignVerticallyTop().alignHorizontallyCenter();

        keySelectionList = new GenericStringSelectionList(
                EMPTY_LIST_LABEL, minecraft, keyNames, getWidthLeft(), getScrollListHeight(), this::onKeySelected);

        LinearLayout inspectorLayout =
                new LinearLayout(getWidthRight(), getScrollListHeight(), LinearLayout.Orientation.VERTICAL);
        inspectorText = new MultiLineTextWidget(
                        Component.translatable("authorisedkeysmc.screen.config.keys.properties-no-selected"),
                        minecraft.font)
                .setMaxWidth(getWidthRight());
        inspectorLayout.addChild(inspectorText);
        inspectorLayout.arrangeElements();

        inspectorScroller = new ScrollableLayout(minecraft, inspectorLayout, inspectorLayout.getHeight());
        inspectorScroller.setMaxHeight(Math.max(scrollHeight, getScrollListHeight()));

        inspectorButtons = new ArrayList<>(4);
        inspectorButtons.add(
                Button.builder(Component.translatable("authorisedkeysmc.button.copy-key"), this::onCopyButtonPressed)
                        .tooltip(Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.share-key")))
                        .size(getWidthRightDual(), 20)
                        .build());
        inspectorButtons.add(Button.builder(
                        Component.translatable("authorisedkeysmc.button.export-key"), this::onBackupButtonPressed)
                .tooltip(Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.backup-key")))
                .size(getWidthRightDual(), 20)
                .build());

        inspectorButtons.add(Button.builder(
                        Component.translatable("authorisedkeysmc.button.password-add"), this::onPasswordButtonPressed)
                .tooltip(Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.set-password")))
                .size(getWidthRightDual(), 20)
                .build());
        passwordButton = inspectorButtons.getLast();

        inspectorButtons.add(Button.builder(
                        Component.translatable("authorisedkeysmc.button.delete-key")
                                .withStyle(ChatFormatting.RED),
                        this::onDeleteButtonPressed)
                .tooltip(Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.delete-key")))
                .size(getWidthRightDual(), 20)
                .build());
        deleteButton = inspectorButtons.getLast();

        inspectorButtons.forEach(button -> button.active = false);

        LinearLayout inspectorUpperFooterLayout = LinearLayout.horizontal().spacing(4);
        inspectorUpperFooterLayout.addChild(inspectorButtons.getFirst());
        inspectorUpperFooterLayout.addChild(inspectorButtons.get(1));

        LinearLayout inspectorLowerFooterLayout = LinearLayout.horizontal().spacing(4);
        inspectorLowerFooterLayout.addChild(inspectorButtons.get(2));
        inspectorLowerFooterLayout.addChild(inspectorButtons.get(3));

        listButtons = new ArrayList<>(2);
        listButtons.add(
                Button.builder(Component.translatable("authorisedkeysmc.button.reload-keys"), button -> reloadKeys())
                        .size(getWidthLeft(), 20)
                        .build());
        listButtons.add(Button.builder(
                        Component.translatable("authorisedkeysmc.button.create-key"), this::onNewKeyButtonPressed)
                .size(getWidthLeft(), 20)
                .build());

        rowHelper.addChild(keySelectionList);
        rowHelper.addChild(inspectorScroller);
        rowHelper.addChild(listButtons.getFirst());
        rowHelper.addChild(inspectorUpperFooterLayout);
        rowHelper.addChild(listButtons.get(1));
        rowHelper.addChild(inspectorLowerFooterLayout);

        contentLayout.arrangeElements();
        forceRecalculateLayout();
        contentLayout.arrangeElements();

        rootLayout.addToHeader(new StringWidget(TITLE_LABEL, font));
        rootLayout.addToContents(contentLayout);
        rootLayout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .width(74 * 2)
                .build());
        rootLayout.visitWidgets(this::addRenderableWidget);

        repositionElements();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        needsLayout = true;
    }

    @Override
    public void repositionElements() {
        rootLayout.arrangeElements();

        forceRecalculateLayout();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(@NonNull GuiGraphics gui, int cursor_x, int cursor_y, float partialTick) {
        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                Screen.MENU_BACKGROUND,
                0,
                this.rootLayout.getHeaderHeight(),
                .0f,
                .0f,
                width,
                rootLayout.getContentHeight(),
                32,
                32);

        recalculateLayoutIfNeeded();

        super.render(gui, cursor_x, cursor_y, partialTick);

        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                Screen.HEADER_SEPARATOR,
                0,
                this.rootLayout.getHeaderHeight() - 2,
                .0f,
                .0f,
                width,
                2,
                32,
                2);

        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                Screen.FOOTER_SEPARATOR,
                0,
                this.height - this.rootLayout.getFooterHeight() - 2,
                .0f,
                .0f,
                width,
                2,
                32,
                2);
    }

    private void onKeySelected(String keyName) {
        inspectorButtons.forEach(button -> button.active = false);

        selectedKeyName = keyName;
        currentKeypair = null;

        needsLayout = true;

        try {
            currentKeypair = AkmcClient.KEY_PAIRS.loadFromFile(selectedKeyName);
        } catch (InvalidPathException e) {
            Constants.LOG.error("Secret key has invalid path \"{}\": {}", selectedKeyName, e);

            inspectorText.setMessage(
                    Component.translatable("authorisedkeysmc.error.key-props", selectedKeyName, e.toString())
                            .withStyle(ChatFormatting.RED));

            return;
        } catch (Exception e) {
            Constants.LOG.error("Could not load secret key \"{}\": {}", selectedKeyName, e);

            inspectorText.setMessage(
                    Component.translatable("authorisedkeysmc.error.key-props", selectedKeyName, e.toString())
                            .withStyle(ChatFormatting.RED));

            deleteButton.active = true;

            return;
        }

        List<String> servers = getServerNamesUsingKey(selectedKeyName);

        MutableComponent message =
                Component.translatable("authorisedkeysmc.screen.config.keys.properties-subtitle", selectedKeyName);

        if (currentKeypair.requiresDecryption()) {
            message.append(Component.translatable("authorisedkeysmc.screen.config.keys.properties-encrypted")
                    .withStyle(ChatFormatting.GREEN));
        }

        message.append(Component.translatable(
                                "authorisedkeysmc.screen.config.keys.properties-time",
                                currentKeypair.getModificationTime().toString())
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("authorisedkeysmc.screen.config.keys.properties-key"))
                .append(Component.literal(currentKeypair.getTextualPublic()).withStyle(ChatFormatting.DARK_PURPLE))
                .append("\n\n")
                .append(Component.translatable("authorisedkeysmc.screen.config.keys.properties-servers"));

        for (int i = 0; i < servers.size(); ++i) {
            if (i % 2 == 0) {
                message.append(" + §7%s§r\n".formatted(servers.get(i)));
            } else {
                message.append(" + %s\n".formatted(servers.get(i)));
            }
        }

        inspectorText.setMessage(message);
        inspectorText.setMaxWidth(getWidthRight());
        inspectorScroller.arrangeElements();

        inspectorButtons.forEach(button -> button.active = true);

        if (currentKeypair.requiresDecryption()) {
            passwordButton.setMessage(Component.translatable("authorisedkeysmc.button.password-change"));
        } else {
            passwordButton.setMessage(Component.translatable("authorisedkeysmc.button.password-add"));
        }
    }

    private void onCopyButtonPressed(Button ignored) {
        if (currentKeypair != null) {
            minecraft.keyboardHandler.setClipboard(currentKeypair.getTextualPublic());
            SystemToast.addOrUpdate(
                    minecraft.getToastManager(),
                    KEY_COPIED_TOAST,
                    Component.translatable("authorisedkeysmc.toast.key-shared"),
                    null);
        }
    }

    private void onNewKeyButtonPressed(Button ignored) {
        minecraft.setScreen(new KeyCreationScreen(KeyManagementScreen.this, this::onNewKeyCreated));
    }

    private void onNewKeyCreated(@Nullable String keyName) {
        reloadKeys();

        if (keyName == null) {
            return;
        }

        try {
            AkKeyPair newPair = AkmcClient.KEY_PAIRS.loadFromFile(keyName);

            if (newPair.requiresDecryption()) {
                minecraft.execute(() -> minecraft.setScreen(new PasswordConfirmPromptScreen(
                        KeyManagementScreen.this, newPair, decrypted -> reloadKeys(), this::onNewKeyCreated)));
            }
        } catch (IOException e) {
            Constants.LOG.warn("Failed to load newly created key: {}", e.getMessage());
        }
    }

    private void onBackupButtonPressed(Button ignored) {
        Constants.LOG.warn("Backing up not implemented!");

        SystemToast.addOrUpdate(minecraft.getToastManager(), WIP_TOAST, Component.literal("Work in progress."), null);
    }

    private void onPasswordButtonPressed(Button ignored) {
        Constants.LOG.warn("Setting/Updating/Erasing password not implemented!");

        SystemToast.addOrUpdate(minecraft.getToastManager(), WIP_TOAST, Component.literal("Work in progress."), null);
    }

    private void onDeleteButtonPressed(Button ignored) {
        if (selectedKeyName == null && currentKeypair == null) {
            return;
        }

        ConfirmScreen screen = new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        if (selectedKeyName != null) {
                            if (currentKeypair != null) {
                                Validate.validState(currentKeypair.getName().equals(selectedKeyName));
                            }

                            AkmcClient.KEY_PAIRS.deleteKeyFile(selectedKeyName);
                        }
                        reloadKeys();
                    }

                    minecraft.setScreen(KeyManagementScreen.this);
                },
                Component.translatable("authorisedkeysmc.screen.delete-key.title")
                        .withStyle(ChatFormatting.RED),
                Component.translatable("authorisedkeysmc.screen.delete-key.prompt"));

        minecraft.setScreen(screen);
    }

    public void reloadKeys() {
        keyNames = AkmcClient.KEY_PAIRS.retrieveKeyNamesFromDisk();
        if (keySelectionList != null) {
            keySelectionList.updateKeyNames(keyNames);
        }
    }

    private int getWidthLeft() {
        float cappedWidth = Math.max(Math.min((float) width * 0.8f, MAX_TOTAL_WIDTH), MIN_TOTAL_WIDTH);
        return Math.round((cappedWidth * WIDTH_LEFT));
    }

    private int getWidthRight() {
        float cappedWidth = Math.max(Math.min((float) width * 0.8f, MAX_TOTAL_WIDTH), MIN_TOTAL_WIDTH);
        return Math.round((cappedWidth * WIDTH_RIGHT));
    }

    private int getWidthRightDual() {
        int wholeRight = getWidthRight();
        return (wholeRight - 4) / 2;
    }

    private int getScrollListHeight() {
        var footerHeight = 40 + 24;
        return rootLayout.getContentHeight() - footerHeight;
    }

    public void recalculateLayoutIfNeeded() {
        if (needsLayout) {
            needsLayout = false;

            repositionElements();
        }
    }

    public void forceRecalculateLayout() {
        contentLayout.setY(rootLayout.getHeaderHeight() + 5);
        keySelectionList.updateSizeAndPosition(
                getWidthLeft(), getScrollListHeight(), contentLayout.getX(), rootLayout.getHeaderHeight() + 5);
        inspectorScroller.setMaxHeight(Math.max(scrollHeight, getScrollListHeight()));
        inspectorText.setMaxWidth(getWidthRight());
        inspectorScroller.setMinWidth(getWidthRight() - 6);
        listButtons.forEach(button -> button.setWidth(getWidthLeft()));
        inspectorButtons.forEach(button -> button.setWidth(getWidthRightDual()));
        contentLayout.arrangeElements();
    }

    private List<String> getServerNamesUsingKey(String keyName) {
        return AkmcClient.KEY_USES.getServersUsingKey(keyName);
    }
}
