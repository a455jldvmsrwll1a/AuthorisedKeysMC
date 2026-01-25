package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.*;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public final class KeyManagementScreen extends BaseScreen {
    private final Screen parent;
    private final HeaderAndFooterLayout rootLayout;
    private final TabManager tabManager;

    private TabNavigationBar tabNav;
    private KeyListTab keyListTab;

    public KeyManagementScreen(Screen parent) {
        super(Component.translatable("authorisedkeysmc.screen.config.title"));

        this.parent = parent;

        rootLayout = new HeaderAndFooterLayout(this);
        tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    }

    protected void init() {
        keyListTab = new KeyListTab();
        tabNav = TabNavigationBar.builder(tabManager, width)
                .addTabs(keyListTab, new ServerListTab())
                .build();
        addRenderableWidget(tabNav);

        LinearLayout linearLayout =
                rootLayout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .width(74 * 2)
                .build());

        rootLayout.visitWidgets(widget -> {
            widget.setTabOrderGroup(1);
            addRenderableWidget(widget);
        });

        tabNav.selectTab(0, false);
        repositionElements();
    }

    @Override
    public void tick() {
        super.tick();

        if (keyListTab != null) {
            keyListTab.tick();
        }
    }

    @Override
    public void repositionElements() {
        if (tabNav != null) {
            tabNav.setWidth(width);
            tabNav.arrangeElements();

            int i = tabNav.getRectangle().bottom();
            ScreenRectangle rect = new ScreenRectangle(0, i, width, height - rootLayout.getFooterHeight() - i);
            tabManager.setTabArea(rect);
            rootLayout.setHeaderHeight(i);
            rootLayout.arrangeElements();
        }

        if (keyListTab != null) {
            keyListTab.forceRecalculateLayout();
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(@NonNull GuiGraphics gui, int cursor_x, int cursor_y, float partialTick) {
        super.render(gui, cursor_x, cursor_y, partialTick);

        keyListTab.recalculateLayoutIfNeeded();

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

    class KeyListTab extends GridLayoutTab {
        private static final float WIDTH_LEFT = 0.433f;
        private static final float WIDTH_RIGHT = 1.f - WIDTH_LEFT;
        private static final float MIN_TOTAL_WIDTH = 388.f;
        private static final float MAX_TOTAL_WIDTH = 800.f;
        private static final Component LIST_HEADER = Component.translatable(
                        "authorisedkeysmc.screen.config.keys.list-header")
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.AQUA);
        private static final Component PROP_HEADER = Component.translatable(
                        "authorisedkeysmc.screen.config.keys.properties-header")
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.AQUA);
        private static final SystemToast.SystemToastId KEY_COPIED_TOAST = new SystemToast.SystemToastId(2000);

        private final int scrollHeight = (font.lineHeight + 2) * 5 - font.lineHeight;

        private List<String> keyNames;

        private final KeySelectionList keySelectionList;
        private final ScrollableLayout inspectorScroller;
        private final MultiLineTextWidget inspectorText;
        private final List<Button> inspectorButtons = new ArrayList<>(3);

        private @Nullable Ed25519PublicKeyParameters currentPubkey;

        private boolean needsLayout = true;

        public KeyListTab() {
            super(Component.translatable("authorisedkeysmc.screen.config.keys.title"));

            reloadKeys();

            GridLayout.RowHelper rowHelper =
                    layout.columnSpacing(8).rowSpacing(8).createRowHelper(2);

            keySelectionList = new KeySelectionList(
                    KeyManagementScreen.this, minecraft, keyNames, getWidthLeft(), getScrollListHeight());

            LinearLayout listFooterLayout = LinearLayout.horizontal().spacing(4);
            listFooterLayout.addChild(Button.builder(
                            Component.translatable("authorisedkeysmc.button.reload-keys"), button -> reloadKeys())
                    .size(90, 20)
                    .build());
            listFooterLayout.addChild(Button.builder(
                            Component.translatable("authorisedkeysmc.button.create-key"),
                            button -> Constants.LOG.warn("Creating not implemented!"))
                    .size(74, 20)
                    .build());

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

            inspectorButtons.add(Button.builder(Component.translatable("authorisedkeysmc.button.copy-key"), button -> {
                        if (currentPubkey != null) {
                            minecraft.keyboardHandler.setClipboard(Base64Util.encode(currentPubkey.getEncoded()));
                            SystemToast.addOrUpdate(
                                    minecraft.getToastManager(),
                                    KEY_COPIED_TOAST,
                                    Component.translatable("authorisedkeysmc.toast.key-shared"),
                                    null);
                        }
                    })
                    .tooltip(Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.share-key")))
                    .size(74, 20)
                    .build());
            inspectorButtons.add(Button.builder(
                            Component.translatable("authorisedkeysmc.button.export-key"),
                            button -> Constants.LOG.warn("Backing up not implemented!"))
                    .tooltip(Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.backup-key")))
                    .size(74, 20)
                    .build());
            inspectorButtons.add(Button.builder(
                            Component.translatable("authorisedkeysmc.button.delete-key"),
                            button -> Constants.LOG.warn("Deleting is not implemented!"))
                    .tooltip(Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.delete-key")))
                    .size(74, 20)
                    .build());

            LinearLayout inspectorFooterLayout = LinearLayout.horizontal().spacing(4);
            inspectorButtons.forEach(button -> {
                button.active = false;
                inspectorFooterLayout.addChild(button);
            });

            rowHelper.addChild(
                    new StringWidget(LIST_HEADER, font).setMaxWidth(getWidthLeft()),
                    LayoutSettings.defaults().paddingTop(4));
            rowHelper.addChild(
                    new StringWidget(PROP_HEADER, font),
                    LayoutSettings.defaults().paddingTop(4));
            rowHelper.addChild(keySelectionList);
            rowHelper.addChild(inspectorScroller);
            rowHelper.addChild(listFooterLayout);
            rowHelper.addChild(inspectorFooterLayout);

            layout.arrangeElements();
            forceRecalculateLayout();
            layout.arrangeElements();
        }

        public void tick() {
            if (!keySelectionList.checkShouldLoad()) {
                return;
            }

            var selected = keySelectionList.getSelected();

            inspectorButtons.forEach(button -> button.active = selected != null);

            if (selected == null) {
                inspectorText.setMessage(
                        Component.translatable("authorisedkeysmc.screen.config.keys.properties-no-selected"));

                return;
            }

            String name = selected.getKeyName();

            Ed25519PrivateKeyParameters secret;
            Instant modificationTime;

            try {
                secret = AuthorisedKeysModClient.KEY_PAIRS.loadFromFile(name);
                modificationTime = AuthorisedKeysModClient.KEY_PAIRS.getModificationTime(name);
            } catch (IOException e) {
                Constants.LOG.error("Could not load secret key \"{}\": {}", name, e);

                inspectorText.setMessage(
                        Component.translatable("authorisedkeysmc.error.key-props", name, e.toString()));

                return;
            }

            currentPubkey = secret.generatePublicKey();
            List<String> hosts = AuthorisedKeysModClient.KNOWN_HOSTS.getHostsUsingKey(name);

            MutableComponent message = Component.translatable(
                            "authorisedkeysmc.screen.config.keys.properties-subtitle", name)
                    .append(Component.translatable(
                            "authorisedkeysmc.screen.config.keys.properties-time", modificationTime.toString()))
                    .append(Component.translatable("authorisedkeysmc.screen.config.keys.properties-key"))
                    .append(Component.literal(Base64Util.encode(currentPubkey.getEncoded())))
                    .append("\n\n")
                    .append(Component.translatable("authorisedkeysmc.screen.config.keys.properties-servers"));
            hosts.forEach(host -> message.append(" + %s\n".formatted(host)));
            inspectorText.setMessage(message);
            inspectorText.setMaxWidth(getWidthRight());
            inspectorScroller.arrangeElements();
        }

        public void recalculateLayoutIfNeeded() {
            if (needsLayout) {
                needsLayout = false;

                forceRecalculateLayout();
            }
        }

        public void forceRecalculateLayout() {
            keySelectionList.updateSizeAndPosition(
                    getWidthLeft(),
                    getScrollListHeight(),
                    layout.getX(),
                    rootLayout.getHeaderHeight() + font.lineHeight + 20);
            inspectorScroller.setMaxHeight(Math.max(scrollHeight, getScrollListHeight()));
            inspectorText.setMaxWidth(getWidthRight());
            layout.arrangeElements();
        }

        @Override
        public void doLayout(@NonNull ScreenRectangle rect) {
            forceRecalculateLayout();

            super.doLayout(rect);
        }

        public void reloadKeys() {
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
            var headerHeight = font.lineHeight + 8 + 8;
            var footerHeight = 20 + 8 + 8;
            return rootLayout.getContentHeight() - headerHeight - footerHeight;
        }
    }

    class ServerListTab extends GridLayoutTab {
        public ServerListTab() {
            super(Component.translatable("authorisedkeysmc.screen.config.servers.title"));

            GridLayout.RowHelper rowHelper = layout.rowSpacing(8).createRowHelper(1);
            rowHelper.addChild(new StringWidget(Component.literal("SERVER LIST TAB"), font));
        }
    }
}
