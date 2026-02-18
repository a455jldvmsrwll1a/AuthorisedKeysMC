package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ServerManagementScreen extends BaseScreen {
    private static final float MIN_TOTAL_WIDTH = 250.0f;
    private static final float MAX_TOTAL_WIDTH = 600.0f;
    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.servers.title")
            .withStyle(ChatFormatting.BOLD)
            .withStyle(ChatFormatting.AQUA);
    private static final Component EDIT_LABEL = Component.translatable("authorisedkeysmc.button.edit");
    private static final Component EMPTY_LIST_LABEL = Component.translatable("authorisedkeysmc.screen.servers.empty");

    private final Screen parent;
    private final HeaderAndFooterLayout rootLayout;
    private final LinearLayout listContainerLayout;
    private final ServerList serverList;

    private GenericStringSelectionList serverSelectionList;
    private Button editButton;

    private final ArrayList<String> serverNames = new ArrayList<>(4);
    private @Nullable String selectedServer;

    private boolean needsLayout = true;

    public ServerManagementScreen(Screen parent) {
        super(Component.translatable("authorisedkeysmc.screen.config.title"));

        this.parent = parent;

        rootLayout = new HeaderAndFooterLayout(this);
        listContainerLayout = LinearLayout.vertical();

        serverList = new ServerList(Minecraft.getInstance());
    }

    protected void init() {
        reloadServers();

        serverSelectionList = new GenericStringSelectionList(
                EMPTY_LIST_LABEL,
                minecraft,
                serverNames,
                getBestWidth(),
                rootLayout.getContentHeight(),
                this::onServerSelected,
                this::onServerDoubleClicked);
        serverSelectionList.borderless = true;

        listContainerLayout.addChild(serverSelectionList);

        LinearLayout buttonsLayout = LinearLayout.horizontal().spacing(4);

        editButton = Button.builder(EDIT_LABEL, this::onEditButtonPressed)
                .size(120, 20)
                .build();

        buttonsLayout.addChild(editButton);
        buttonsLayout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .size(120, 20)
                .build());

        rootLayout.addToHeader(new StringWidget(TITLE_LABEL, font));
        rootLayout.addToContents(listContainerLayout);
        rootLayout.addToFooter(buttonsLayout);
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
                this.height - this.rootLayout.getFooterHeight(),
                .0f,
                .0f,
                width,
                2,
                32,
                2);
    }

    private void onServerSelected(String serverName) {
        var selected = serverSelectionList.getSelected();
        if (selected == null) {
            editButton.active = false;
            selectedServer = null;

            return;
        }

        selectedServer = selected.getKeyName();
        editButton.active = true;
    }

    private void onServerDoubleClicked(String serverName) {
        ServerData data;
        for (int i = 0; i < serverList.size(); ++i) {
            data = serverList.get(i);

            if (data.name.equals(serverName)) {
                minecraft.setScreen(new ServerInfoScreen(this, data));

                return;
            }
        }
    }

    private void onEditButtonPressed(Button button) {
        ServerData data;
        for (int i = 0; i < serverList.size(); ++i) {
            data = serverList.get(i);

            if (data.name.equals(selectedServer)) {
                minecraft.setScreen(new ServerInfoScreen(this, data));

                return;
            }
        }
    }

    private void reloadServers() {
        serverList.load();

        serverNames.clear();
        for (int i = 0; i < serverList.size(); ++i) {
            serverNames.add(serverList.get(i).name);
        }

        if (serverSelectionList != null) {
            serverSelectionList.updateKeyNames(serverNames);
        }
    }

    private int getBestWidth() {
        float cappedWidth = Math.max(Math.min((float) width * 0.8f, MAX_TOTAL_WIDTH), MIN_TOTAL_WIDTH);
        return Math.round(cappedWidth);
    }

    public void recalculateLayoutIfNeeded() {
        if (needsLayout) {
            needsLayout = false;

            repositionElements();
        }
    }

    public void forceRecalculateLayout() {
        serverSelectionList.updateSizeAndPosition(
                getBestWidth(),
                rootLayout.getContentHeight(),
                listContainerLayout.getX(),
                rootLayout.getHeaderHeight());

        rootLayout.arrangeElements();
    }
}
