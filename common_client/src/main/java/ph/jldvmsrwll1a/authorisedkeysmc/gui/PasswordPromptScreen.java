package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import ph.jldvmsrwll1a.authorisedkeysmc.LoadedKeypair;

public class PasswordPromptScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 74;
    private static final int HORIZONTAL_SPACE = 60;
    private static final int MAX_WIDTH = 550;
    private static final int MAX_PASSWORD_LENGTH = 255;

    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.decrypt-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component PROMPT_LABEL = Component.translatable("authorisedkeysmc.screen.decrypt-key.prompt");
    private static final Component DECRYPT_BUTTON_LABEL = Component.translatable("authorisedkeysmc.button.decrypt-key");
    private static final Component PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.password");
    private static final Component SHOW_PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.field.show-password");
    private static final Component ERROR_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.error").withStyle(ChatFormatting.RED);
    private static final Component WAITING_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.waiting");

    private final Screen parent;
    private final LoadedKeypair keypair;
    private final Consumer<LoadedKeypair> callback;
    private final LinearLayout rootLayout;
    private final AtomicBoolean showPassword = new AtomicBoolean(false);

    private MultiLineTextWidget promptText;
    private EditBox passwordEdit;
    private StringWidget errorText;

    public PasswordPromptScreen(Screen parent, LoadedKeypair keypair, Consumer<LoadedKeypair> callback) {
        super(TITLE_LABEL);

        Validate.isTrue(keypair.requiresDecryption(), "requesting password for an unencrypted key");

        this.parent = parent;
        this.keypair = keypair;
        this.callback = callback;

        rootLayout = LinearLayout.vertical().spacing(4);
    }

    @Override
    protected void init() {
        StringWidget passwordLabel = new StringWidget(PASSWORD_LABEL, font);

        promptText = new MultiLineTextWidget(
                Component.translatable("authorisedkeysmc.screen.decrypt-key.prompt", keypair.getName()), font);

        passwordEdit = new EditBox(font, 300, 20, PASSWORD_LABEL);
        passwordEdit.setResponder(this::onPasswordChanged);
        passwordEdit.addFormatter(new PasswordPromptScreen.PasswordTextFormatter(showPassword));
        passwordEdit.setMaxLength(MAX_PASSWORD_LENGTH + 1);

        Checkbox showPasswordCheckbox = Checkbox.builder(SHOW_PASSWORD_LABEL, font)
                .onValueChange(this::onShowPasswordCheckboxChanged)
                .build();

        errorText = new StringWidget(ERROR_LABEL, font);
        errorText.visible = false;

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.defaultCellSetting().paddingTop(16);

        Button decryptKeyButton = Button.builder(DECRYPT_BUTTON_LABEL, button -> decryptKey())
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
    protected void repositionElements() {
        promptText.setMaxWidth(elementWidth());
        passwordEdit.setWidth(elementWidth());

        rootLayout.arrangeElements();
        FrameLayout.centerInRectangle(rootLayout, getRectangle());
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (passwordEdit.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            decryptKey();

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
        callback.accept(keypair);
    }

    private void onPasswordChanged(String ignored) {
        errorText.visible = false;
    }

    private void onShowPasswordCheckboxChanged(Checkbox checkbox, boolean b) {
        showPassword.setRelease(b);
    }

    private void decryptKey() {
        minecraft.setScreenAndShow(new GenericMessageScreen(WAITING_LABEL));

        if (keypair.decrypt(passwordEdit.getValue())) {
            minecraft.setScreen(parent);
            onClose();

            return;
        }

        errorText.visible = true;
        minecraft.setScreen(this);
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }

    private record PasswordTextFormatter(AtomicBoolean shouldShow) implements EditBox.TextFormatter {
        @Override
        public @NonNull FormattedCharSequence format(@NonNull String s, int i) {
            if (shouldShow.getAcquire()) {
                return FormattedCharSequence.forward(s, Style.EMPTY);
            }

            StringBuilder sb = new StringBuilder(s.length());
            // 'M' is a pretty wide character in the default font.
            s.chars().forEach(c -> sb.append('M'));
            return FormattedCharSequence.forward(sb.toString(), Style.EMPTY.withObfuscated(true));
        }
    }
}
