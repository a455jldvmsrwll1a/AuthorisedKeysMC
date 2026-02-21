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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public final class PasswordUpdateScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 74;
    private static final int HORIZONTAL_SPACE = 60;
    private static final int MAX_WIDTH = 550;
    private static final int MAX_PASSWORD_LENGTH = 255;

    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.update-password.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component ERASE_CONFIRM_TITLE_LABEL = Component.translatable(
                    "authorisedkeysmc.screen.erase-password.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
    private static final Component UPDATE_BUTTON_LABEL =
            Component.translatable("authorisedkeysmc.button.update-password");
    private static final Component ERASE_BUTTON_LABEL =
            Component.translatable("authorisedkeysmc.button.erase-password").withStyle(ChatFormatting.RED);
    private static final Component CURRENT_PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.update-password.current-password");
    private static final Component NEW_PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.update-password.new-password");
    private static final Component NEW_PASSWORD_HINT_LABEL =
            Component.translatable("authorisedkeysmc.screen.update-password.new-password-hint");
    private static final Component ERROR_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.error").withStyle(ChatFormatting.RED);
    private static final Component WAIT_DECRYPT_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.waiting");
    private static final Component WAIT_ENCRYPT_LABEL =
            Component.translatable("authorisedkeysmc.screen.add-password.waiting");
    private static final Identifier SHOW_PASSWORD_ICON = Constants.modId("widget/show_password");
    private static final Identifier HIDE_PASSWORD_ICON = Constants.modId("widget/hide_password");
    private static final Tooltip SHOW_PASSWORD_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.show-password"));
    private static final Tooltip HIDE_PASSWORD_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.hide-password"));

    private final Screen parent;
    private final AkKeyPair.Encrypted keypair;
    private final Consumer<Optional<? extends AkKeyPair>> callback;
    private final LinearLayout rootLayout;
    private final AtomicBoolean showCurrentPassword = new AtomicBoolean(false);
    private final AtomicBoolean showNewPassword = new AtomicBoolean(false);

    private MultiLineTextWidget promptText;
    private EditBox currentPasswordEdit;
    private IconButton showCurrentPasswordButton;
    private EditBox newPasswordEdit;
    private IconButton showNewPasswordButton;
    private StringWidget errorText;
    private Button actionButton;

    public PasswordUpdateScreen(
            Screen parent, AkKeyPair.Encrypted keypair, Consumer<Optional<? extends AkKeyPair>> callback) {
        super(TITLE_LABEL);

        this.parent = parent;
        this.keypair = keypair;
        this.callback = callback;

        rootLayout = LinearLayout.vertical().spacing(4);
    }

    @Override
    protected void init() {
        promptText = new MultiLineTextWidget(
                Component.translatable("authorisedkeysmc.screen.update-password.prompt", keypair.getName()), font);

        currentPasswordEdit = new EditBox(font, 100, 20, CURRENT_PASSWORD_LABEL);
        currentPasswordEdit.setResponder(this::onCurrentPasswordChanged);
        currentPasswordEdit.addFormatter(new PasswordPromptScreen.PasswordTextFormatter(showCurrentPassword));
        currentPasswordEdit.setMaxLength(MAX_PASSWORD_LENGTH + 1);

        showCurrentPasswordButton = IconButton.builder(SHOW_PASSWORD_ICON, this::onShowCurrentPasswordButtonClicked)
                .tooltip(SHOW_PASSWORD_TOOLTIP)
                .build();

        LinearLayout currentPasswordLayout = LinearLayout.horizontal().spacing(4);
        currentPasswordLayout.addChild(currentPasswordEdit);
        currentPasswordLayout.addChild(showCurrentPasswordButton);

        newPasswordEdit = new EditBox(font, 100, 20, CURRENT_PASSWORD_LABEL);
        newPasswordEdit.setResponder(this::onNewPasswordChanged);
        newPasswordEdit.addFormatter(new PasswordPromptScreen.PasswordTextFormatter(showNewPassword));
        newPasswordEdit.setMaxLength(MAX_PASSWORD_LENGTH + 1);
        newPasswordEdit.setHint(NEW_PASSWORD_HINT_LABEL);

        showNewPasswordButton = IconButton.builder(SHOW_PASSWORD_ICON, this::onShowNewPasswordButtonClicked)
                .tooltip(SHOW_PASSWORD_TOOLTIP)
                .build();

        LinearLayout newPasswordLayout = LinearLayout.horizontal().spacing(4);
        newPasswordLayout.addChild(newPasswordEdit);
        newPasswordLayout.addChild(showNewPasswordButton);

        errorText = new StringWidget(ERROR_LABEL, font);
        errorText.visible = false;

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.defaultCellSetting().paddingTop(16);

        actionButton = Button.builder(ERASE_BUTTON_LABEL, button -> reencryptKey())
                .width(120)
                .build();

        buttonLayout.addChild(actionButton);
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .width(BUTTON_WIDTH)
                .build());

        rootLayout.addChild(new StringWidget(getTitle(), font));
        rootLayout.addChild(promptText);
        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(new StringWidget(CURRENT_PASSWORD_LABEL, font));
        rootLayout.addChild(currentPasswordLayout);
        rootLayout.addChild(new StringWidget(NEW_PASSWORD_LABEL, font));
        rootLayout.addChild(newPasswordLayout);
        rootLayout.addChild(errorText);
        rootLayout.addChild(buttonLayout);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        promptText.setMaxWidth(elementWidth());
        currentPasswordEdit.setWidth(elementWidth() - 24);
        newPasswordEdit.setWidth(elementWidth() - 24);

        rootLayout.arrangeElements();
        FrameLayout.centerInRectangle(rootLayout, getRectangle());
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        boolean submit = event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER;

        if (currentPasswordEdit.isFocused() && submit) {
            setFocused(newPasswordEdit);

            return true;
        }

        if (newPasswordEdit.isFocused() && submit) {
            reencryptKey();

            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(currentPasswordEdit);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void onCurrentPasswordChanged(String password) {
        if (!password.isEmpty()) {
            errorText.visible = false;
        }
    }

    private void onNewPasswordChanged(String password) {
        if (password.isEmpty()) {
            actionButton.setMessage(ERASE_BUTTON_LABEL);
        } else {
            actionButton.setMessage(UPDATE_BUTTON_LABEL);
        }
    }

    private void onShowCurrentPasswordButtonClicked(Button button) {
        boolean show = !showCurrentPassword.getAcquire();

        if (show) {
            showCurrentPasswordButton.setSprite(HIDE_PASSWORD_ICON);
            showCurrentPasswordButton.setTooltip(HIDE_PASSWORD_TOOLTIP);
        } else {
            showCurrentPasswordButton.setSprite(SHOW_PASSWORD_ICON);
            showCurrentPasswordButton.setTooltip(SHOW_PASSWORD_TOOLTIP);
        }

        showCurrentPassword.setRelease(show);
    }

    private void onShowNewPasswordButtonClicked(Button button) {
        boolean show = !showNewPassword.getAcquire();

        if (show) {
            showNewPasswordButton.setSprite(HIDE_PASSWORD_ICON);
            showNewPasswordButton.setTooltip(HIDE_PASSWORD_TOOLTIP);
        } else {
            showNewPasswordButton.setSprite(SHOW_PASSWORD_ICON);
            showNewPasswordButton.setTooltip(SHOW_PASSWORD_TOOLTIP);
        }

        showNewPassword.setRelease(show);
    }

    private void reencryptKey() {
        char[] currentPassword = currentPasswordEdit.getValue().toCharArray();
        char[] newPassword = newPasswordEdit.getValue().toCharArray();

        AkmcClient.WORKER_EXECUTOR.execute(() -> {
            try {
                minecraft.execute(() -> minecraft.setScreen(new GenericMessageScreen(WAIT_DECRYPT_LABEL)));

                Optional<AkKeyPair.Plain> decrypted = keypair.decrypt(currentPassword);

                if (decrypted.isEmpty()) {
                    minecraft.execute(() -> {
                        errorText.visible = true;
                        currentPasswordEdit.setValue("");
                        minecraft.setScreen(this);
                    });

                    return;
                }

                if (newPassword.length == 0) {
                    minecraft.execute(() -> {
                        AkKeyPair.Plain kp = decrypted.get();

                        minecraft.setScreen(new ConfirmScreen(
                                confirmed -> {
                                    callback.accept(confirmed ? Optional.of(kp) : Optional.empty());
                                    onClose();
                                },
                                ERASE_CONFIRM_TITLE_LABEL,
                                Component.translatable("authorisedkeysmc.screen.erase-password.prompt", kp.getName())));
                    });

                    return;
                }

                minecraft.execute(() -> minecraft.setScreen(new GenericMessageScreen(WAIT_ENCRYPT_LABEL)));

                AkKeyPair.Encrypted reencrypted = decrypted.get().encrypt(newPassword);

                minecraft.execute(() -> minecraft.setScreen(new PasswordConfirmPromptScreen(
                        parent,
                        reencrypted,
                        encrypted -> encrypted.ifPresentOrElse(
                                e -> callback.accept(Optional.of(e)), () -> callback.accept(Optional.empty())))));
            } catch (RuntimeException e) {
                minecraft.execute(() -> {
                    errorText.visible = true;
                    currentPasswordEdit.setValue("");
                    minecraft.setScreen(this);
                });

                throw e;
            } finally {
                Arrays.fill(currentPassword, '\0');
                Arrays.fill(newPassword, '\0');
            }
        });
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }
}
