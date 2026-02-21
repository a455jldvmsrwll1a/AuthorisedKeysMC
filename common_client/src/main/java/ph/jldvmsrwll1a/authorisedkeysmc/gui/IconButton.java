package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class IconButton extends Button {
    private final int padding;

    private Identifier sprite;
    private float fade = 1.0f;

    private IconButton(
            int x,
            int y,
            int width,
            int height,
            int padding,
            Identifier sprite,
            OnPress onPress,
            CreateNarration narration) {
        super(x, y, width, height, Component.empty(), onPress, narration);

        this.sprite = sprite;
        this.padding = padding;
    }

    public static Builder builder(Identifier sprite, OnPress callback) {
        return new Builder(sprite, callback);
    }

    public void setSprite(Identifier sprite) {
        this.sprite = sprite;
    }

    public void setFade(float fade) {
        this.fade = fade;
    }

    @Override
    protected void renderContents(@NonNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int spriteX = getX() + padding;
        int spriteY = getY() + padding;
        int spriteW = getWidth() - padding * 2;
        int spriteH = getHeight() - padding * 2;

        renderDefaultSprite(gui);
        gui.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, spriteX, spriteY, spriteW, spriteH, fade);
    }

    public static final class Builder {
        private final Identifier sprite;
        private final OnPress callback;
        private @Nullable Tooltip tooltip;
        private int x = 0;
        private int y = 0;
        private int width = 20;
        private int height = 20;
        private int padding = 2;

        public Builder(Identifier sprite, OnPress callback) {
            this.sprite = sprite;
            this.callback = callback;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;

            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;

            return this;
        }

        public Builder padding(int padding) {
            this.padding = padding;

            return this;
        }

        public IconButton build() {
            IconButton button =
                    new IconButton(x, y, width, height, padding, sprite, callback, Button.DEFAULT_NARRATION);
            button.setTooltip(tooltip);

            return button;
        }
    }
}
