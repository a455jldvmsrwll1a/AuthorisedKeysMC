package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class KeySelectionList extends ObjectSelectionList<KeySelectionList.KeyListEntry> {
    private boolean listIsEmpty;
    private @Nullable KeyListEntry lastSelection = null;
    private boolean shouldLoad = false;

    public KeySelectionList(Screen screen, Minecraft minecraft, List<String> keyNames, int width, int height) {
        super(minecraft, width, height, 0, minecraft.font.lineHeight * 2);

        updateKeyNames(keyNames);
    }

    public void updateKeyNames(@Nullable List<String> keyNames) {
        clearEntries();
        shouldLoad = true;
        setSelected(null);

        listIsEmpty = keyNames == null || keyNames.isEmpty();
        if (listIsEmpty) {
            return;
        }

        for (String name : keyNames) {
            addEntry(new KeyListEntry(minecraft, name));
        }
    }

    @Override
    public void setSelected(@Nullable KeyListEntry selected) {
        super.setSelected(selected);
        if (lastSelection == null || lastSelection != selected) {
            lastSelection = selected;
            shouldLoad = true;
        }
    }

    public boolean checkShouldLoad() {
        boolean should = shouldLoad;
        shouldLoad = false;

        return should;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - 6;
    }

    @Override
    public void updateSizeAndPosition(int width, int height, int x, int y) {
        super.updateSizeAndPosition(width, height, x, y);
    }

    @Override
    protected void renderSelection(GuiGraphics gui, KeyListEntry entry, int colour) {
        int cappedWidth = Math.min(width - 8, entry.getWidth());

        int centreX = entry.getX() + entry.getWidth() / 2;
        int left = centreX - cappedWidth / 2;
        int right = centreX + cappedWidth / 2;

        int bottom = entry.getY();
        int top = bottom + entry.getHeight();

        gui.fill(left, bottom, right, top, colour);
        gui.fill(left + 1, bottom + 1, right - 1, top - 1, 0xFF000000);
    }

    @Override
    public void renderWidget(@NonNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(gui, mouseX, mouseY, partialTick);

        if (listIsEmpty) {
            int x = getX() + getWidth() / 2;
            int y = getY() + getHeight() / 2;
            gui.drawCenteredString(
                    minecraft.font,
                    Component.translatable("authorisedkeysmc.screen.config.keys.empty-list-message"),
                    x,
                    y,
                    0xFFFFFFFF);
        }
    }

    public static final class KeyListEntry extends ObjectSelectionList.Entry<KeyListEntry> {
        private final StringWidget stringWidget;
        private final String keyName;

        public KeyListEntry(@NotNull Minecraft minecraft, @NotNull String keyName) {
            this.keyName = keyName;
            stringWidget = new StringWidget(Component.literal(keyName), minecraft.font);
        }

        public String getKeyName() {
            return keyName;
        }

        @Override
        public @NonNull Component getNarration() {
            return stringWidget.getMessage();
        }

        @Override
        public void renderContent(
                @NonNull GuiGraphics gui, int mouseX, int mouseY, boolean hovering, float partialTick) {
            stringWidget.setPosition(
                    getContentXMiddle() - stringWidget.getWidth() / 2,
                    getContentYMiddle() - stringWidget.getHeight() / 2);
            stringWidget.render(gui, mouseX, mouseY, partialTick);
        }
    }
}
