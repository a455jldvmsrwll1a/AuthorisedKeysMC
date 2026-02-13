package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class JoinKeySelectionScreen extends KeySelectionScreen {
    private static final Component PROMPT_LABEL =
            Component.translatable("authorisedkeysmc.screen.join-select-key.prompt");

    public JoinKeySelectionScreen(Screen parent, Consumer<@Nullable String> consumer) {
        super(parent, consumer, PROMPT_LABEL);
    }
}
