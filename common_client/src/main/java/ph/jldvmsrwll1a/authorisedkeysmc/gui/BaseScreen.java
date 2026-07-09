package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Version;

public abstract class BaseScreen extends Screen {
    private static final String LABEL = "AKMC " + Version.getProjectVersion();

    protected BaseScreen(Component title) {
        super(title);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gui, mouseX, mouseY, partialTick);

        int colour = AkmcClient.PLATFORM.isDevelopmentEnvironment() ? 0xFFFF00FF : 0xFF7F7F7F;
        gui.text(font, LABEL, 2, height - font.lineHeight - 1, colour, true);
    }
}
