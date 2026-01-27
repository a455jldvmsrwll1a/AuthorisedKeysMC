package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.ClientKeyPairs;
import ph.jldvmsrwll1a.authorisedkeysmc.util.ValidPath;

public class KeyCreationScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 74;
    private static final int HORIZONTAL_SPACE = 60;
    private static final int MAX_WIDTH = 550;
    private static final int MAX_PASSWORD_LENGTH = 255;

    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component PREAMBLE_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.preamble");
    private static final Component GEN_BUTTON_LABEL = Component.translatable("authorisedkeysmc.button.generate-key");
    private static final Component NAME_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.field.name");
    private static final Component PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.field.password");
    private static final Component ENCRYPT_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.field.encrypt");
    private static final Component SHOW_PASSWORD_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.field.show-password");
    private static final Component ENCRYPTION_REMINDER_LABEL = Component.translatable(
                    "authorisedkeysmc.screen.new-key.encryption-reminder")
            .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
    private static final Component NAME_TAKEN_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.name-taken").withStyle(ChatFormatting.RED);
    private static final Component WAITING_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.waiting");

    private final Screen parent;
    private final Consumer<String> callback;
    private final List<String> existingNames;
    private final LinearLayout rootLayout;

    private ScrollableLayout scrollLayout;
    private MultiLineTextWidget preambleText;
    private MultiLineTextWidget fileLocationText;
    private MultiLineTextWidget reminderText;
    private StringWidget passwordLabel;
    private EditBox nameEdit;
    private EditBox passwordEdit;
    private Checkbox passwordCheckbox;
    private Checkbox showPasswordCheckbox;
    private Button genKeybutton;

    private String currentName;
    private String currentPassword;
    private String lastNameError = "";
    private final AtomicBoolean showPassword = new AtomicBoolean(false);

    public KeyCreationScreen(Screen parent, Consumer<String> callback) {
        super(TITLE_LABEL);

        this.parent = parent;
        this.callback = callback;

        existingNames = AuthorisedKeysModClient.KEY_PAIRS.retrieveKeyNamesFromDisk();
        rootLayout = LinearLayout.vertical().spacing(4);
    }

    @Override
    protected void init() {
        StringWidget nameLabel = new StringWidget(NAME_LABEL, font);

        passwordLabel = new StringWidget(PASSWORD_LABEL, font);
        passwordLabel.setHeight(0);
        passwordLabel.visible = false;

        preambleText = new MultiLineTextWidget(PREAMBLE_LABEL, font);
        fileLocationText = new MultiLineTextWidget(Component.empty(), font);
        reminderText = new MultiLineTextWidget(Component.empty(), font);

        nameEdit = new EditBox(font, 300, 20, NAME_LABEL);
        nameEdit.setHint(Component.literal("default"));
        nameEdit.setResponder(this::onNameChanged);
        nameEdit.setMaxLength(ValidPath.MAX_LENGTH + 1);

        passwordEdit = new EditBox(font, 300, 0, PASSWORD_LABEL);
        passwordEdit.setResponder(this::onPasswordChanged);
        passwordEdit.addFormatter(new PasswordTextFormatter(showPassword));
        passwordEdit.setMaxLength(MAX_PASSWORD_LENGTH + 1);
        passwordEdit.active = false;
        passwordEdit.visible = false;

        passwordCheckbox = Checkbox.builder(ENCRYPT_LABEL, font)
                .onValueChange(this::onEncryptionCheckboxChanged)
                .build();

        showPasswordCheckbox = Checkbox.builder(SHOW_PASSWORD_LABEL, font)
                .onValueChange(this::onShowPasswordCheckboxChanged)
                .build();
        showPasswordCheckbox.visible = false;
        showPasswordCheckbox.setHeight(0);

        LinearLayout buttonLayout = LinearLayout.horizontal().spacing(4);
        buttonLayout.defaultCellSetting().paddingTop(16);

        genKeybutton = Button.builder(GEN_BUTTON_LABEL, button -> createNewKey())
                .width(BUTTON_WIDTH)
                .build();
        genKeybutton.active = false;

        buttonLayout.addChild(genKeybutton);
        buttonLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .width(BUTTON_WIDTH)
                .build());

        LinearLayout scrollContentsLayout = LinearLayout.vertical().spacing(4);

        scrollContentsLayout.addChild(preambleText);
        scrollContentsLayout.addChild(new SpacerElement(1, font.lineHeight));
        scrollContentsLayout.addChild(nameLabel);
        scrollContentsLayout.addChild(nameEdit);
        scrollContentsLayout.addChild(fileLocationText);
        scrollContentsLayout.addChild(new SpacerElement(1, font.lineHeight));
        scrollContentsLayout.addChild(passwordCheckbox);
        scrollContentsLayout.addChild(reminderText);
        scrollContentsLayout.addChild(passwordLabel);
        scrollContentsLayout.addChild(passwordEdit);
        scrollContentsLayout.addChild(showPasswordCheckbox);

        scrollLayout = new ScrollableLayout(minecraft, scrollContentsLayout, scrollContentsLayout.getHeight());

        rootLayout.addChild(new StringWidget(TITLE_LABEL, font));
        rootLayout.addChild(scrollLayout);
        rootLayout.addChild(buttonLayout);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();

        if (existingNames.isEmpty()) {
            nameEdit.setValue("default");
        }
    }

    @Override
    protected void repositionElements() {
        scrollLayout.setMaxHeight(height - 100);

        preambleText.setMaxWidth(elementWidth());
        fileLocationText.setMaxWidth(elementWidth());
        reminderText.setMaxWidth(elementWidth());

        nameEdit.setWidth(elementWidth());
        passwordEdit.setWidth(elementWidth());

        rootLayout.arrangeElements();
        FrameLayout.centerInRectangle(rootLayout, getRectangle());
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(nameEdit);
    }

    @Override
    public void resize(int x, int y) {
        super.resize(x, y);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(@NonNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void onNameChanged(String name) {
        currentName = name;
        onInputUpdate();
    }

    private void onPasswordChanged(String password) {
        currentPassword = password;
        onInputUpdate();
    }

    private void onEncryptionCheckboxChanged(Checkbox checkbox, boolean checked) {
        passwordEdit.active = checked;

        reminderText.setMessage(checked ? ENCRYPTION_REMINDER_LABEL : Component.empty());

        passwordLabel.visible = checked;
        passwordEdit.visible = checked;
        showPasswordCheckbox.visible = checked;

        passwordLabel.setHeight(checked ? font.lineHeight : 0);
        passwordEdit.setHeight(checked ? 20 : 0);
        showPasswordCheckbox.setHeight(checked ? 20 : 0);

        onInputUpdate();
        repositionElements();
    }

    private void onShowPasswordCheckboxChanged(Checkbox checkbox, boolean b) {
        showPassword.setRelease(b);
    }

    private void onInputUpdate() {
        genKeybutton.active = inputsAreValid();

        if (!nameIsValid()) {
            Component label = Component.translatable("authorisedkeysmc.screen.new-key.invalid-name", lastNameError)
                    .withStyle(ChatFormatting.RED);
            fileLocationText.setMessage(label);
        } else if (!nameIsFree()) {
            fileLocationText.setMessage(NAME_TAKEN_LABEL);
        } else {
            fileLocationText.setMessage(makeLocationLabel());
        }
    }

    private boolean nameIsValid() {
        if (currentName == null || currentName.isBlank()) {
            lastNameError = "Can't be empty.";

            return false;
        }

        try {
            // Try to construct a path with the name to ensure that the file can actually be written.
            Path ignored = ClientKeyPairs.fromKeyName(currentName);
        } catch (InvalidPathException e) {
            lastNameError = e.getReason();

            return false;
        }

        return true;
    }

    private boolean nameIsFree() {
        return !existingNames.contains(currentName);
    }

    private boolean passwordIsValid() {
        return !passwordCheckbox.selected()
                || (currentPassword != null
                        && !currentPassword.isEmpty()
                        && currentPassword.length() <= MAX_PASSWORD_LENGTH);
    }

    private boolean inputsAreValid() {
        return nameIsValid() && passwordIsValid() && nameIsFree();
    }

    private void createNewKey() {
        if (!inputsAreValid()) {
            return;
        }

        minecraft.setScreenAndShow(new GenericMessageScreen(WAITING_LABEL));

        minecraft.executeBlocking(() -> {
            AuthorisedKeysModClient.KEY_PAIRS.generate(currentName, currentPassword);

            callback.accept(currentName);
            onClose();
        });
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }

    private Component makeLocationLabel() {
        char sep = File.separatorChar;
        String format = "%s%s%s%s.der".formatted(sep, Constants.MOD_DIR_NAME, sep, currentName);
        return Component.translatable("authorisedkeysmc.screen.new-key.file-location", format)
                .withStyle(ChatFormatting.GRAY);
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
