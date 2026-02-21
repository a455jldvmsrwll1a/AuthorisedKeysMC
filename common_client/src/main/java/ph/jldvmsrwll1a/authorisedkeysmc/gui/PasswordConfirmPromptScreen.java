package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public final class PasswordConfirmPromptScreen extends PasswordPromptScreen {
    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.confirm-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component CONFIRM_BUTTON_LABEL = Component.translatable("authorisedkeysmc.button.confirm");
    private static final Component PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.password");
    private static final Component SHOW_PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.field.show-password");
    private static final Component ERROR_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.error").withStyle(ChatFormatting.RED);
    private static final Component WAITING_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.waiting");

    private final Consumer<Optional<AkKeyPair.Encrypted>> callback;
    boolean successful = false;

    public PasswordConfirmPromptScreen(
            Screen parent, AkKeyPair.Encrypted keypair, Consumer<Optional<AkKeyPair.Encrypted>> callback) {
        super(parent, keypair, kp -> {});

        this.callback = callback;
    }

    @Override
    protected void init() {
        StringWidget passwordLabel = new StringWidget(PASSWORD_LABEL, font);

        promptText = new MultiLineTextWidget(
                Component.translatable("authorisedkeysmc.screen.confirm-key.prompt", keypair.getName()), font);

        passwordEdit = new EditBox(font, 300, 20, PASSWORD_LABEL);
        passwordEdit.setResponder(super::onPasswordChanged);
        passwordEdit.addFormatter(new PasswordPromptScreen.PasswordTextFormatter(showPassword));
        passwordEdit.setMaxLength(MAX_PASSWORD_LENGTH + 1);

        Checkbox showPasswordCheckbox = Checkbox.builder(SHOW_PASSWORD_LABEL, font)
                .onValueChange(this::onShowPasswordCheckboxChanged)
                .build();

        errorText = new StringWidget(ERROR_LABEL, font);
        errorText.visible = false;

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.defaultCellSetting().paddingTop(16);

        Button decryptKeyButton = Button.builder(CONFIRM_BUTTON_LABEL, button -> decryptKey())
                .width(BUTTON_WIDTH)
                .build();

        buttonLayout.addChild(decryptKeyButton);
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .width(BUTTON_WIDTH)
                .build());

        rootLayout.addChild(new StringWidget(TITLE_LABEL, font));
        rootLayout.addChild(promptText);
        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(passwordLabel);
        rootLayout.addChild(passwordEdit);
        rootLayout.addChild(showPasswordCheckbox);
        rootLayout.addChild(errorText);
        rootLayout.addChild(buttonLayout);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();
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
