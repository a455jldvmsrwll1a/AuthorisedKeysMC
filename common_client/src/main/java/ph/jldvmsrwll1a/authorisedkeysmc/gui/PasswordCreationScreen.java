package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public final class PasswordCreationScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 74;
    private static final int HORIZONTAL_SPACE = 60;
    private static final int MAX_WIDTH = 550;
    private static final int MAX_PASSWORD_LENGTH = 255;

    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.add-password.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component ENCRYPT_BUTTON_LABEL = Component.translatable("authorisedkeysmc.button.add");
    private static final Component PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.password");
    private static final Component SHOW_PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.field.show-password");
    private static final Component WAITING_LABEL =
            Component.translatable("authorisedkeysmc.screen.add-password.waiting");
    private static final Component ERROR_LABEL = Component.translatable("authorisedkeysmc.error.encryption-fail");

    private final Screen parent;
    private final AkKeyPair.Plain keypair;
    private final Consumer<Optional<AkKeyPair.Encrypted>> callback;
    private final LinearLayout rootLayout;
    private final AtomicBoolean showPassword = new AtomicBoolean(false);

    private MultiLineTextWidget promptText;
    private EditBox passwordEdit;
    private Button encryptKeyButton;

    public PasswordCreationScreen(
            Screen parent, AkKeyPair.Plain keypair, Consumer<Optional<AkKeyPair.Encrypted>> callback) {
        super(TITLE_LABEL);

        this.parent = parent;
        this.keypair = keypair;
        this.callback = callback;

        rootLayout = LinearLayout.vertical().spacing(4);
    }

    @Override
    protected void init() {
        StringWidget passwordLabel = new StringWidget(PASSWORD_LABEL, font);

        promptText = new MultiLineTextWidget(
                Component.translatable("authorisedkeysmc.screen.add-password.prompt", keypair.getName()), font);

        passwordEdit = new EditBox(font, 300, 20, PASSWORD_LABEL);
        passwordEdit.setResponder(this::onPasswordChanged);
        passwordEdit.addFormatter(new PasswordPromptScreen.PasswordTextFormatter(showPassword));
        passwordEdit.setMaxLength(MAX_PASSWORD_LENGTH + 1);

        Checkbox showPasswordCheckbox = Checkbox.builder(SHOW_PASSWORD_LABEL, font)
                .onValueChange(this::onShowPasswordCheckboxChanged)
                .build();

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.defaultCellSetting().paddingTop(16);

        encryptKeyButton = Button.builder(ENCRYPT_BUTTON_LABEL, button -> encryptKey())
                .width(BUTTON_WIDTH)
                .build();

        buttonLayout.addChild(encryptKeyButton);
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .width(BUTTON_WIDTH)
                .build());

        rootLayout.addChild(new StringWidget(TITLE_LABEL, font));
        rootLayout.addChild(promptText);
        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(passwordLabel);
        rootLayout.addChild(passwordEdit);
        rootLayout.addChild(showPasswordCheckbox);
        rootLayout.addChild(buttonLayout);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        promptText.setMaxWidth(elementWidth());
        passwordEdit.setWidth(elementWidth());

        rootLayout.arrangeElements();
        FrameLayout.centerInRectangle(rootLayout, getRectangle());
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (passwordEdit.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            encryptKey();

            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(passwordEdit);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void onPasswordChanged(String password) {
        encryptKeyButton.active = !password.isEmpty();
    }

    private void onShowPasswordCheckboxChanged(Checkbox checkbox, boolean b) {
        showPassword.setRelease(b);
    }

    private void encryptKey() {
        if (passwordEdit.getValue().isEmpty()) {
            return;
        }

        minecraft.setScreen(new GenericMessageScreen(WAITING_LABEL));

        char[] password = passwordEdit.getValue().toCharArray();
        AkmcClient.WORKER_EXECUTOR.execute(() -> {
            AkKeyPair.Encrypted encrypted;
            try {
                encrypted = keypair.encrypt(password);
            } catch (RuntimeException e) {
                minecraft.execute(
                        () -> minecraft.setScreen(new ErrorScreen(ERROR_LABEL, Component.literal(e.getMessage()))));

                throw e;
            } finally {
                Arrays.fill(password, '\0');
            }

            minecraft.execute(() -> minecraft.setScreen(new PasswordConfirmPromptScreen(parent, encrypted, callback)));
        });
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }
}
