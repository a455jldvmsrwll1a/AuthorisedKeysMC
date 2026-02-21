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
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;

public class PasswordPromptScreen extends BaseScreen {
    protected static final int BUTTON_WIDTH = 74;
    protected static final int HORIZONTAL_SPACE = 60;
    protected static final int MAX_WIDTH = 550;
    protected static final int MAX_PASSWORD_LENGTH = 255;

    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.decrypt-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component DECRYPT_BUTTON_LABEL = Component.translatable("authorisedkeysmc.button.decrypt-key");
    private static final Component PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.password");
    private static final Component CACHE_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.cache-key");
    private static final Component CACHE_TEXT_LABEL = Component.translatable(
                    "authorisedkeysmc.screen.decrypt-key.cache-key-text")
            .withStyle(ChatFormatting.GOLD);
    private static final Component ERROR_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.error").withStyle(ChatFormatting.RED);
    private static final Component WAITING_LABEL =
            Component.translatable("authorisedkeysmc.screen.decrypt-key.waiting");
    private static final Identifier SHOW_PASSWORD_ICON = Constants.modId("widget/show_password");
    private static final Identifier HIDE_PASSWORD_ICON = Constants.modId("widget/hide_password");
    private static final Tooltip SHOW_PASSWORD_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.show-password"));
    private static final Tooltip HIDE_PASSWORD_TOOLTIP =
            Tooltip.create(Component.translatable("authorisedkeysmc.tooltip.hide-password"));

    protected final Screen parent;
    protected final AkKeyPair.Encrypted keypair;
    protected final Consumer<Optional<AkKeyPair.Plain>> callback;
    protected final LinearLayout rootLayout;
    protected final AtomicBoolean showPassword = new AtomicBoolean(false);

    protected MultiLineTextWidget promptText;
    protected EditBox passwordEdit;
    protected IconButton showPasswordButton;
    protected StringWidget errorText;
    protected MultiLineTextWidget cacheText;
    protected boolean cacheDecryptedKey = false;

    private AkKeyPair.@Nullable Plain decryptedKey = null;

    public PasswordPromptScreen(
            Screen parent, AkKeyPair.Encrypted keypair, Consumer<Optional<AkKeyPair.Plain>> callback) {
        this(TITLE_LABEL, parent, keypair, callback);
    }

    public PasswordPromptScreen(
            Component title, Screen parent, AkKeyPair.Encrypted keypair, Consumer<Optional<AkKeyPair.Plain>> callback) {
        super(title);

        this.parent = parent;
        this.keypair = keypair;
        this.callback = callback;

        rootLayout = LinearLayout.vertical().spacing(4);
    }

    @Override
    protected final void init() {
        StringWidget passwordLabel = new StringWidget(PASSWORD_LABEL, font);

        promptText = new MultiLineTextWidget(getPrompt(), font);

        passwordEdit = new EditBox(font, 100, 20, PASSWORD_LABEL);
        passwordEdit.setResponder(this::onPasswordChanged);
        passwordEdit.addFormatter(new PasswordPromptScreen.PasswordTextFormatter(showPassword));
        passwordEdit.setMaxLength(MAX_PASSWORD_LENGTH + 1);

        showPasswordButton = IconButton.builder(SHOW_PASSWORD_ICON, this::onShowPasswordButtonClicked)
                .tooltip(SHOW_PASSWORD_TOOLTIP)
                .build();

        LinearLayout passwordLayout = LinearLayout.horizontal().spacing(4);
        passwordLayout.addChild(passwordEdit);
        passwordLayout.addChild(showPasswordButton);

        Checkbox cacheCheckbox = Checkbox.builder(CACHE_LABEL, font)
                .onValueChange(this::onCacheCheckboxChanged)
                .build();

        errorText = new StringWidget(ERROR_LABEL, font);
        errorText.visible = false;

        cacheText = new MultiLineTextWidget(CACHE_TEXT_LABEL, font);
        cacheText.visible = false;

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.defaultCellSetting().paddingTop(16);

        Button decryptKeyButton = Button.builder(DECRYPT_BUTTON_LABEL, button -> decryptKey())
                .width(BUTTON_WIDTH)
                .build();

        buttonLayout.addChild(decryptKeyButton);
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .width(BUTTON_WIDTH)
                .build());

        rootLayout.addChild(new StringWidget(getTitle(), font));
        rootLayout.addChild(promptText);
        rootLayout.addChild(new SpacerElement(1, font.lineHeight));
        rootLayout.addChild(passwordLabel);
        rootLayout.addChild(passwordLayout);
        rootLayout.addChild(cacheCheckbox);
        rootLayout.addChild(errorText);
        rootLayout.addChild(cacheText);
        rootLayout.addChild(buttonLayout);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected final void repositionElements() {
        promptText.setMaxWidth(elementWidth());
        passwordEdit.setWidth(elementWidth() - 24);

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
        callback.accept(Optional.ofNullable(decryptedKey));

        minecraft.setScreen(parent);
    }

    protected Component getPrompt() {
        return Component.translatable("authorisedkeysmc.screen.decrypt-key.prompt", keypair.getName());
    }

    protected void onPasswordChanged(String password) {
        if (!password.isEmpty()) {
            errorText.visible = false;
        }
    }

    protected void onShowPasswordButtonClicked(Button button) {
        boolean show = !showPassword.getAcquire();

        if (show) {
            showPasswordButton.setSprite(HIDE_PASSWORD_ICON);
            showPasswordButton.setTooltip(HIDE_PASSWORD_TOOLTIP);
        } else {
            showPasswordButton.setSprite(SHOW_PASSWORD_ICON);
            showPasswordButton.setTooltip(SHOW_PASSWORD_TOOLTIP);
        }

        showPassword.setRelease(show);
    }

    protected void onCacheCheckboxChanged(Checkbox checkbox, boolean b) {
        cacheDecryptedKey = b;
        cacheText.visible = b;
    }

    protected void decryptKey() {
        minecraft.setScreen(new GenericMessageScreen(WAITING_LABEL));

        char[] password = passwordEdit.getValue().toCharArray();
        AkmcClient.WORKER_EXECUTOR.execute(() -> {
            try {
                keypair.decrypt(password).ifPresent(decrypted -> decryptedKey = decrypted);
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

            if (decryptedKey != null && cacheDecryptedKey) {
                AkmcClient.CACHED_KEYS.cacheKey(decryptedKey);
            }

            minecraft.execute(() -> {
                if (decryptedKey != null) {
                    minecraft.execute(this::onClose);
                } else {
                    errorText.visible = true;
                    passwordEdit.setValue("");
                    minecraft.setScreen(this);
                }
            });
        });
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }

    protected record PasswordTextFormatter(AtomicBoolean shouldShow) implements EditBox.TextFormatter {
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
