package ph.jldvmsrwll1a.authorisedkeysmc.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.UserKeys;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;

public final class ModCommands {
    private static final int SUCCESS = 1;
    private static final int ERROR = -1;

    private ModCommands() {}

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            Commands.CommandSelection environment) {
        if (environment != Commands.CommandSelection.DEDICATED) {
            return;
        }

        dispatcher.register(literal("akmc")
                .then(literal("status").executes(ModCommands::hello))
                .then(literal("reload").requires(ModCommands::admin).executes(ModCommands::reload))
                .then(literal("enable").requires(ModCommands::admin).executes(ModCommands::enable))
                .then(literal("disable").requires(ModCommands::admin).executes(ModCommands::disable))
                .then(literal("bind")
                        .then(argument("key", StringArgumentType.string())
                                .executes(ModCommands::bind)))
                .then(literal("unbind")
                        .then(argument("key", StringArgumentType.string())
                                .suggests(new SelfKeysSuggestions())
                                .executes(ModCommands::unbind)))
                .then(literal("id")
                        .requires(ModCommands::admin)
                        .then(argument("id", UuidArgument.uuid())
                                .suggests(new RegisteredUserIdSuggestions())
                                .executes(ModCommands::idInfo)
                                .then(literal("bind")
                                        .then(argument("key", StringArgumentType.string())
                                                .executes(ModCommands::idBind)))
                                .then(literal("unbind")
                                        .then(argument("key", StringArgumentType.string())
                                                .executes(ModCommands::idUnbind))))));
    }

    private static int hello(CommandContext<CommandSourceStack> context) {
        MutableComponent message =  Component.empty()
                .append(Component.literal("== AuthorisedKeysMC ==")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("\n\nStatus: "));

        if (AuthorisedKeysModCore.CONFIG.enforcing) {
            message.append(Component.literal("ENFORCING").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            message.append(Component.literal("ON STANDBY").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        reply(context, message);

        return SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        try {
            AuthorisedKeysModCore.reload();
        } catch (Exception e) {
            Constants.LOG.error("Could not run reload command: {}", e);
            reply(
                    context,
                    Component.literal("Failed to reload: %s".formatted(e.toString()))
                            .withStyle(ChatFormatting.RED));

            return ERROR;
        }

        context.getSource().sendSuccess(() -> Component.literal("AKMC reloaded!").withStyle(ChatFormatting.GREEN), true);

        return SUCCESS;
    }

    private static int enable(CommandContext<CommandSourceStack> context) {
        if (AuthorisedKeysModCore.CONFIG.enforcing) {
            reply(context, Component.literal("AKMC is already enforcing.").withStyle(ChatFormatting.RED));

            return ERROR;
        }

        AuthorisedKeysModCore.CONFIG.enforcing = true;
        AuthorisedKeysModCore.CONFIG.write();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("AKMC is now ")
                                .append(Component.literal("ENFORCING")
                                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)),
                        true);

        return SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> context) {
        if (!AuthorisedKeysModCore.CONFIG.enforcing) {
            reply(context, Component.literal("AKMC is already on standby.").withStyle(ChatFormatting.RED));

            return ERROR;
        }

        AuthorisedKeysModCore.CONFIG.enforcing = false;
        AuthorisedKeysModCore.CONFIG.write();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("AKMC is now ")
                                .append(Component.literal("ON STANDBY")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)),
                        true);

        return SUCCESS;
    }

    private static int bind(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "Must be executed by a player! To bind a key to a specific user, use: /akmc user <user> bind <key>"));
            return ERROR;
        }

        String encodedKey = StringArgumentType.getString(context, "key");
        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            context.getSource()
                    .sendFailure(
                            Component.literal("Invalid public key string. Make sure you copy-pasted it correctly."));
            return ERROR;
        }

        if (AuthorisedKeysModCore.USER_KEYS.bindKey(player.getUUID(), player.getUUID(), key)) {
            reply(context, Component.literal("Bound your key!").withStyle(ChatFormatting.GREEN));

            return SUCCESS;
        } else {
            reply(context, Component.literal("You have already bound this key.").withStyle(ChatFormatting.RED));
            return ERROR;
        }
    }

    private static int unbind(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "Must be executed by a player! To unbind a specific user, use: /akmc user <user> unbind <key>"));
            return ERROR;
        }

        String encodedKey = StringArgumentType.getString(context, "key");
        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            context.getSource()
                    .sendFailure(
                            Component.literal("Invalid public key string. Make sure you copy-pasted it correctly."));
            return ERROR;
        }

        if (AuthorisedKeysModCore.USER_KEYS.unbindKey(player.getUUID(), key)) {
            reply(
                    context,
                    Component.literal("Unbound your key: ")
                            .append(Component.literal(encodedKey).withStyle(ChatFormatting.DARK_PURPLE)));

            return SUCCESS;
        } else {
            reply(context, Component.literal("You have no such key.").withStyle(ChatFormatting.RED));
            return ERROR;
        }
    }

    private static int idInfo(CommandContext<CommandSourceStack> context) {
        UUID id = UuidArgument.getUuid(context, "id");

        List<UserKeys.UserKey> keys = AuthorisedKeysModCore.USER_KEYS.getUserKeys(id);

        if (keys == null || keys.isEmpty()) {
            reply(
                    context,
                    Component.literal("No user by that UUID is on record.").withStyle(ChatFormatting.GRAY));

            return SUCCESS;
        }

        ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(id);

        MutableComponent message = Component.empty();
        message.append(Component.literal(String.valueOf(keys.size())).withStyle(ChatFormatting.AQUA));
        message.append(" key");
        if (keys.size() != 1) {
            message.append("s");
        }
        message.append(" belong");
        if (keys.size() == 1) {
            message.append("s");
        }
        message.append(" to ");
        if (player == null) {
            message.append(id.toString());
        } else {
            message.append(player.getDisplayName());
        }
        if (keys.isEmpty()) {
            message.append(".");
        } else {
            message.append(":");
        }
        keys.forEach(key -> {
            message.append("\n");

            String keyString = key.key.toString();
            message.append(Component.literal(keyString)
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy.")))
                            .withClickEvent(new ClickEvent.CopyToClipboard(keyString))));
        });

        reply(context, message);

        return SUCCESS;
    }

    private static int idBind(CommandContext<CommandSourceStack> context) {
        UUID id = UuidArgument.getUuid(context, "id");
        String encodedKey = StringArgumentType.getString(context, "key");

        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            context.getSource()
                    .sendFailure(
                            Component.literal("Invalid public key string. Make sure you copy-pasted it correctly."));
            return ERROR;
        }

        ServerPlayer player = context.getSource().getPlayer();
        UUID issuer = player != null ? player.getUUID() : null;

        if (AuthorisedKeysModCore.USER_KEYS.bindKey(id, issuer, key)) {
            reply(context, Component.literal("Key was successfully bound!"));

            return SUCCESS;
        } else {
            reply(context, Component.literal("This user already has that key.").withStyle(ChatFormatting.RED));

            return ERROR;
        }
    }

    private static int idUnbind(CommandContext<CommandSourceStack> context) {
        UUID id = UuidArgument.getUuid(context, "id");
        String encodedKey = StringArgumentType.getString(context, "key");

        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            context.getSource()
                    .sendFailure(
                            Component.literal("Invalid public key string. Make sure you copy-pasted it correctly."));
            return ERROR;
        }

        if (AuthorisedKeysModCore.USER_KEYS.unbindKey(id, key)) {
            reply(context, "Key was successfully unbound!");

            return SUCCESS;
        } else {
            reply(context, Component.literal("No such key or user.").withStyle(ChatFormatting.RED));
            return ERROR;
        }
    }

    private static int b44werwrsse4ind(CommandContext<CommandSourceStack> context) {

        return SUCCESS;
    }

    private static boolean admin(CommandSourceStack source) {
        return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS));
    }

    private static void reply(CommandContext<CommandSourceStack> context, String message) {
        reply(context, Component.literal(message));
    }

    private static void reply(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSystemMessage(message);
    }
}
