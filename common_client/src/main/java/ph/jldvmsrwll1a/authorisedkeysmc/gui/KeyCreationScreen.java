package ph.jldvmsrwll1a.authorisedkeysmc.gui;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcClient;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;
import ph.jldvmsrwll1a.authorisedkeysmc.key.ClientKeyPairs;
import ph.jldvmsrwll1a.authorisedkeysmc.util.ValidPath;

public class KeyCreationScreen extends BaseScreen {
    private static final int BUTTON_WIDTH = 74;
    private static final int HORIZONTAL_SPACE = 60;
    private static final int MAX_WIDTH = 550;

    private static final Component TITLE_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
    private static final Component PREAMBLE_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.preamble");
    private static final Component GEN_BUTTON_LABEL = Component.translatable("authorisedkeysmc.button.generate-key");
    private static final Component NAME_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.field.name");
    private static final Component ENCRYPT_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.field.encrypt");
    private static final Component NAME_TAKEN_LABEL =
            Component.translatable("authorisedkeysmc.screen.new-key.name-taken").withStyle(ChatFormatting.RED);
    private static final Component WAITING_LABEL = Component.translatable("authorisedkeysmc.screen.new-key.waiting");

    private final Screen parent;
    private final Consumer<Optional<? extends AkKeyPair>> callback;
    private final List<String> existingNames;
    private final LinearLayout rootLayout;

    private ScrollableLayout scrollLayout;
    private MultiLineTextWidget preambleText;
    private MultiLineTextWidget fileLocationText;
    private EditBox nameEdit;
    private Checkbox passwordCheckbox;
    private Button genKeybutton;

    private String currentName;
    private String lastNameError = "";

    public KeyCreationScreen(Screen parent, Consumer<Optional<? extends AkKeyPair>> callback) {
        this(parent, callback, null);
    }

    public KeyCreationScreen(
            Screen parent, Consumer<Optional<? extends AkKeyPair>> callback, @Nullable String defaultName) {
        super(TITLE_LABEL);

        this.parent = parent;
        this.callback = callback;

        existingNames = AkmcClient.KEY_PAIRS.retrieveKeyNamesFromDisk();
        rootLayout = LinearLayout.vertical().spacing(4);
        currentName = defaultName;
    }

    @Override
    protected void init() {
        StringWidget nameLabel = new StringWidget(NAME_LABEL, font);

        preambleText = new MultiLineTextWidget(PREAMBLE_LABEL, font);
        fileLocationText = new MultiLineTextWidget(Component.empty(), font);

        nameEdit = new EditBox(font, 300, 20, NAME_LABEL);
        nameEdit.setHint(Component.literal("default"));
        nameEdit.setMaxLength(ValidPath.MAX_LENGTH + 1);
        if (currentName != null) {
            // We set the value before any responder is set so we do not run our callbacks prematurely.
            nameEdit.setValue(currentName);
        }
        nameEdit.setResponder(this::onNameChanged);

        passwordCheckbox = Checkbox.builder(ENCRYPT_LABEL, font).build();

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

        scrollLayout = new ScrollableLayout(minecraft, scrollContentsLayout, scrollContentsLayout.getHeight());

        rootLayout.addChild(new StringWidget(TITLE_LABEL, font));
        rootLayout.addChild(scrollLayout);
        rootLayout.addChild(buttonLayout);

        rootLayout.visitWidgets(this::addRenderableWidget);
        repositionElements();

        if (existingNames.isEmpty()) {
            nameEdit.setValue("default");
        }

        // Bit of a hack to make the location hint text update if the screen was initialised with a default name.
        if (currentName != null && !currentName.isEmpty()) {
            onNameChanged(currentName);
        }
    }

    @Override
    protected void repositionElements() {
        scrollLayout.setMaxHeight(height - 100);
        preambleText.setMaxWidth(elementWidth());
        fileLocationText.setMaxWidth(elementWidth());
        nameEdit.setWidth(elementWidth());

        rootLayout.arrangeElements();
        FrameLayout.centerInRectangle(rootLayout, getRectangle());
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(nameEdit);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void onNameChanged(String name) {
        currentName = name;
        onInputUpdate();
    }

    private void onInputUpdate() {
        genKeybutton.active = nameIsValid() && nameIsFree();

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

    private void createNewKey() {
        if (!nameIsValid() || !nameIsFree()) {
            return;
        }

        minecraft.setScreen(new GenericMessageScreen(WAITING_LABEL));

        AkmcClient.WORKER_EXECUTOR.execute(() -> {
            try {
                AkKeyPair.Plain keyPair = AkKeyPair.generate(SecureRandom.getInstanceStrong(), currentName);

                if (passwordCheckbox.selected()) {
                    minecraft.execute(() -> minecraft.setScreen(new PasswordCreationScreen(
                            parent,
                            keyPair,
                            encrypted -> encrypted.ifPresentOrElse(
                                    e -> callback.accept(Optional.of(e)), () -> callback.accept(Optional.empty())))));
                } else {
                    minecraft.execute(() -> {
                        callback.accept(Optional.of(keyPair));
                        onClose();
                    });
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to generate keypair: {}", e.getMessage());
                minecraft.execute(() -> minecraft.setScreen(new ErrorScreen(
                        Component.translatable("authorisedkeysmc.error.generation-fail"),
                        Component.literal(e.getMessage()))));
            }
        });
    }

    private int elementWidth() {
        return Math.min(width - HORIZONTAL_SPACE, MAX_WIDTH);
    }

    private Component makeLocationLabel() {
        char sep = File.separatorChar;
        Path keysDir = AkmcClient.FILE_PATHS.KEY_PAIRS_DIR.getFileName();
        String format = "%s%s%s%s%s%s%s"
                .formatted(sep, Constants.MOD_DIR_NAME, sep, keysDir, sep, currentName, Constants.KEY_PAIR_EXTENSION);
        return Component.translatable("authorisedkeysmc.screen.new-key.file-location", format)
                .withStyle(ChatFormatting.GRAY);
    }
}
