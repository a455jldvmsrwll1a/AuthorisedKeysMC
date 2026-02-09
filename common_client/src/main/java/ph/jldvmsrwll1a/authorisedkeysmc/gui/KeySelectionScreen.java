package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.LoadedKeypair;

public class KeySelectionScreen extends BaseScreen {
    private static final float WIDTH_LEFT = 4.0f / 11.0f;
    private static final float WIDTH_RIGHT = 1.f - WIDTH_LEFT;
    private static final float MIN_TOTAL_WIDTH = 350.0f;
    private static final float MAX_TOTAL_WIDTH = 600.0f;
    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.select-key.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.AQUA);
    private static final Component PROMPT_LABEL = Component.translatable("authorisedkeysmc.screen.select-key.prompt");
    private static final Component EMPTY_LIST_LABEL =
            Component.translatable("authorisedkeysmc.screen.config.keys.empty-list-message");
    private static final Component SELECT_LABEL = Component.translatable("authorisedkeysmc.button.select");

    private final Screen parent;
    private final HeaderAndFooterLayout rootLayout;
    private final GridLayout contentLayout;
    private final Consumer<@Nullable String> consumer;

    private final int scrollHeight = (font.lineHeight + 2) * 5 - font.lineHeight;

    private List<String> keyNames;

    private GenericStringSelectionList keySelectionList;
    private ScrollableLayout inspectorScroller;
    private MultiLineTextWidget inspectorText;
    private Button selectButton;

    private @Nullable String keyName;
    private boolean hasSelection = false;

    private boolean needsLayout = true;

    public KeySelectionScreen(Screen parent, Consumer<@Nullable String> consumer) {
        super(TITLE_LABEL);

        this.parent = parent;
        this.consumer = consumer;

        rootLayout = new HeaderAndFooterLayout(this);
        contentLayout = new GridLayout();

        ServerList serverList = new ServerList(Minecraft.getInstance());
        serverList.load();
    }

    protected void init() {
        reloadKeys();

        GridLayout.RowHelper rowHelper =
                contentLayout.columnSpacing(16).rowSpacing(4).createRowHelper(2);
        rowHelper.defaultCellSetting().alignVerticallyTop().alignHorizontallyCenter();

        keySelectionList = new GenericStringSelectionList(
                EMPTY_LIST_LABEL, minecraft, keyNames, getWidthLeft(), getScrollListHeight());

        LinearLayout inspectorLayout =
                new LinearLayout(getWidthRight(), getScrollListHeight(), LinearLayout.Orientation.VERTICAL);
        inspectorText = new MultiLineTextWidget(
                        Component.translatable("authorisedkeysmc.screen.select-key.no-keys"), minecraft.font)
                .setMaxWidth(getWidthRight());
        inspectorLayout.addChild(inspectorText);
        inspectorLayout.arrangeElements();

        inspectorScroller = new ScrollableLayout(minecraft, inspectorLayout, inspectorLayout.getHeight());
        inspectorScroller.setMaxHeight(Math.max(scrollHeight, getScrollListHeight()));

        StringWidget promptWidget = new StringWidget(PROMPT_LABEL, font);
        promptWidget.setMaxWidth(Math.round(MIN_TOTAL_WIDTH * width), StringWidget.TextOverflow.SCROLLING);
        rowHelper.addChild(promptWidget, 2);
        rowHelper.addChild(SpacerElement.height(5), 2);
        rowHelper.addChild(keySelectionList);
        rowHelper.addChild(inspectorScroller);

        contentLayout.arrangeElements();
        forceRecalculateLayout();

        selectButton = Button.builder(SELECT_LABEL, this::onSelectButtonPressed)
                .width(74)
                .build();
        selectButton.active = false;

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.addChild(selectButton);
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .width(74)
                .build());

        rootLayout.addToHeader(new StringWidget(TITLE_LABEL, font));
        rootLayout.addToContents(contentLayout);
        rootLayout.addToFooter(buttonLayout);
        rootLayout.visitWidgets(this::addRenderableWidget);

        repositionElements();
    }

    @Override
    public void tick() {
        super.tick();

        if (!keySelectionList.checkShouldLoad()) {
            return;
        }

        selectButton.active = false;

        var selected = keySelectionList.getSelected();

        if (selected == null) {
            inspectorText.setMessage(Component.translatable("authorisedkeysmc.screen.select-key.no-keys"));

            return;
        }

        keyName = selected.getKeyName();
        LoadedKeypair currentKeypair = null;

        needsLayout = true;

        try {
            currentKeypair = AuthorisedKeysModClient.KEY_PAIRS.loadFromFile(keyName);
        } catch (InvalidPathException e) {
            Constants.LOG.error("Secret key has invalid path \"{}\": {}", keyName, e);

            inspectorText.setMessage(Component.translatable("authorisedkeysmc.error.key-props", keyName, e.toString())
                    .withStyle(ChatFormatting.RED));

            return;
        } catch (Exception e) {
            Constants.LOG.error("Could not load secret key \"{}\": {}", keyName, e);

            inspectorText.setMessage(Component.translatable("authorisedkeysmc.error.key-props", keyName, e.toString())
                    .withStyle(ChatFormatting.RED));

            selectButton.active = true;

            return;
        }

        List<String> servers = getServerNamesUsingKey(keyName);

        MutableComponent message =
                Component.translatable("authorisedkeysmc.screen.config.keys.properties-subtitle", keyName);

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
        selectButton.active = true;
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
        consumer.accept(hasSelection ? keyName : null);
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

    private void onSelectButtonPressed(Button button) {
        if (keyName != null) {
            hasSelection = true;

            onClose();
        }
    }

    private void reloadKeys() {
        keyNames = AuthorisedKeysModClient.KEY_PAIRS.retrieveKeyNamesFromDisk();
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

    private int getScrollListHeight() {
        return rootLayout.getContentHeight() - 24 - font.lineHeight;
    }

    public void recalculateLayoutIfNeeded() {
        if (needsLayout) {
            needsLayout = false;

            repositionElements();
        }
    }

    public void forceRecalculateLayout() {
        contentLayout.setY(rootLayout.getHeaderHeight() + 4);
        keySelectionList.updateSizeAndPosition(
                getWidthLeft(), getScrollListHeight(), contentLayout.getX(), rootLayout.getHeaderHeight() + 26);
        inspectorScroller.setMaxHeight(Math.max(scrollHeight, getScrollListHeight()));
        inspectorText.setMaxWidth(getWidthRight());
        contentLayout.arrangeElements();
    }

    private List<String> getServerNamesUsingKey(String keyName) {
        return AuthorisedKeysModClient.KEY_USES.getServersUsingKey(keyName);
    }
}
