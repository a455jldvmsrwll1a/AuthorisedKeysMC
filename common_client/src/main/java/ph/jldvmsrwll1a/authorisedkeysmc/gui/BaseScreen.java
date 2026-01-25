package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public abstract class BaseScreen extends Screen {
    protected BaseScreen(Component title) {
        super(title);
    }

    @Override
    public void render(@NonNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);

        gui.drawString(font, Constants.MOD_NAME, 2, height - font.lineHeight - 1, 0xFF00FFFF, true);
    }
}
