package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public final class PasswordConfirmPromptScreen extends PasswordPromptScreen {
    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.confirm-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component WAITING_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.waiting");

    private final Consumer<Optional<AkKeyPair.Encrypted>> callback;
    boolean successful = false;

    public PasswordConfirmPromptScreen(
            Screen parent, AkKeyPair.Encrypted keypair, Consumer<Optional<AkKeyPair.Encrypted>> callback) {
        super(TITLE_LABEL, parent, keypair, kp -> {});

        this.callback = callback;
    }

    @Override
    protected Component getPrompt() {
        return Component.translatable("authorisedkeysmc.screen.confirm-key.prompt", keypair.getName());
    }

    @Override
    public void onClose() {
        callback.accept(Optional.ofNullable(successful ? keypair : null));

        minecraft.setScreen(parent);
    }

    @Override
    protected void decryptKey() {
        minecraft.setScreen(new GenericMessageScreen(WAITING_LABEL));

        char[] password = passwordEdit.getValue().toCharArray();
        AkmcClient.WORKER_EXECUTOR.execute(() -> {
            try {
                if (keypair.decrypt(password).isPresent()) {
                    successful = true;
                }
            } catch (RuntimeException e) {
                minecraft.execute(() -> {
                    errorText.visible = true;
                    passwordEdit.setValue("");
                    minecraft.setScreen(this);
                });

                throw e;
            } finally {
                Arrays.fill(password, '\0');
            }

            minecraft.execute(() -> {
                if (successful) {
                    minecraft.execute(this::onClose);
                } else {
                    errorText.visible = true;
                    passwordEdit.setValue("");
                    minecraft.setScreen(this);
                }
            });
        });
    }
}
