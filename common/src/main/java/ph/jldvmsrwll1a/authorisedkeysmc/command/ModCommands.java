package ph.jldvmsrwll1a.authorisedkeysmc.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
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
                .then(literal("info").executes(ModCommands::selfInfo))
                .then(literal("bind")
                        .then(argument("public key", StringArgumentType.word()).executes(ModCommands::bind)))
                .then(literal("unbind")
                        .then(argument("public key", StringArgumentType.word())
                                .suggests(new PublicKeysSuggestions.Self())
                                .executes(ModCommands::unbind)))
                .then(literal("user")
                        .requires(ModCommands::admin)
                        .then(argument("username", StringArgumentType.word())
                                .suggests(new UsernameSuggestions())
                                .executes(ModCommands::usernameInfo)
                                .then(literal("bind")
                                        .then(argument("public key", StringArgumentType.word())
                                                .executes(ModCommands::usernameBind)))
                                .then(literal("unbind")
                                        .then(argument("public key", StringArgumentType.word())
                                                .suggests(new PublicKeysSuggestions.ByUsername())
                                                .executes(ModCommands::usernameUnbind))))));
    }

    private static int hello(CommandContext<CommandSourceStack> context) {
        MutableComponent message = Component.empty()
                .append(Component.literal("== AuthorisedKeysMC ==").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("\n\nStatus: "));

        if (AkmcCore.CONFIG.enforcing) {
            message.append(Component.literal("ENFORCING").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            message.append(Component.literal("ON STANDBY").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        reply(context, message);

        return SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        try {
            AkmcCore.reload();
        } catch (Exception e) {
            Constants.LOG.error("Could not run reload command: {}", e);
            fail(context, "Failed to reload: %s".formatted(e.toString()));

            return ERROR;
        }

        context.getSource()
                .sendSuccess(() -> Component.literal("AKMC reloaded!").withStyle(ChatFormatting.GREEN), true);

        return SUCCESS;
    }

    private static int enable(CommandContext<CommandSourceStack> context) {
        if (AkmcCore.CONFIG.enforcing) {
            fail(context, "AKMC is already enforcing.");

            return ERROR;
        }

        AkmcCore.CONFIG.enforcing = true;
        AkmcCore.CONFIG.write();

        context.getSource()
                .sendSuccess(
                        () -> Component.literal("AKMC is now ")
                                .append(Component.literal("ENFORCING")
                                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)),
                        true);

        return SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> context) {
        if (!AkmcCore.CONFIG.enforcing) {
            fail(context, "AKMC is already on standby.");

            return ERROR;
        }

        AkmcCore.CONFIG.enforcing = false;
        AkmcCore.CONFIG.write();

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
            fail(
                    context,
                    "Must be executed by a player! To bind a key to a specific user, use: /akmc user <username> bind <public key>");

            return ERROR;
        }

        String encodedKey = StringArgumentType.getString(context, "public key");
        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            fail(context, "Invalid public key string. Make sure you copy-pasted it correctly.");

            return ERROR;
        }

        if (AkmcCore.USER_KEYS.bindKey(player.getPlainTextName(), player.getPlainTextName(), key)) {
            reply(context, "Bound your key!", ChatFormatting.GREEN);

            return SUCCESS;
        } else {
            fail(context, "You have already bound this key.");

            return ERROR;
        }
    }

    private static int unbind(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            fail(
                    context,
                    "Must be executed by a player! To unbind a key from a specific user, use: /akmc user <username> unbind <public key>");

            return ERROR;
        }

        String encodedKey = StringArgumentType.getString(context, "public key");
        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            fail(context, "Invalid public key string. Make sure you copy-pasted it correctly.");

            return ERROR;
        }

        if (AkmcCore.USER_KEYS.unbindKey(player.getPlainTextName(), key)) {
            reply(context, "Unbound your key!", ChatFormatting.GREEN);

            return SUCCESS;
        } else {
            fail(context, "You have no such key.");

            return ERROR;
        }
    }

    private static int selfInfo(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            fail(context, "Must be executed by a player! To query a specific user, use: /akmc user <username> info");

            return ERROR;
        }

        return playerInfo(context, context.getSource().getPlayer().getPlainTextName());
    }

    private static int usernameInfo(CommandContext<CommandSourceStack> context) {
        return playerInfo(context, StringArgumentType.getString(context, "username"));
    }

    private static int usernameBind(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "username");
        String encodedKey = StringArgumentType.getString(context, "public key");

        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            fail(context, "Invalid public key string. Make sure you copy-pasted it correctly.");

            return ERROR;
        }

        ServerPlayer player = context.getSource().getPlayer();

        if (AkmcCore.USER_KEYS.bindKey(username, player != null ? player.getPlainTextName() : null, key)) {
            reply(context, "Bound this key to %s!".formatted(username), ChatFormatting.GREEN);

            ServerPlayer targetPlayer =
                    context.getSource().getServer().getPlayerList().getPlayer(username);
            if (targetPlayer != null) {
                String keyString = key.toString();

                if (player != null) {
                    targetPlayer.sendSystemMessage(Component.empty()
                            .append(player.getDisplayName())
                            .append(" bound a new key to your username: ")
                            .append(Component.literal(keyString)
                                    .withStyle(Style.EMPTY
                                            .withColor(ChatFormatting.GOLD)
                                            .withHoverEvent(
                                                    new HoverEvent.ShowText(Component.literal("Click to copy.")))
                                            .withClickEvent(new ClickEvent.CopyToClipboard(keyString)))));
                } else {
                    targetPlayer.sendSystemMessage(Component.literal("A new key has been bound to your username: ")
                            .append(Component.literal(keyString)
                                    .withStyle(Style.EMPTY
                                            .withColor(ChatFormatting.GOLD)
                                            .withHoverEvent(
                                                    new HoverEvent.ShowText(Component.literal("Click to copy.")))
                                            .withClickEvent(new ClickEvent.CopyToClipboard(keyString)))));
                }
            }

            return SUCCESS;
        } else {
            fail(context, "This user already has that key.");

            return ERROR;
        }
    }

    private static int usernameUnbind(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "username");
        String encodedKey = StringArgumentType.getString(context, "public key");

        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            fail(context, "Invalid public key string. Make sure you copy-pasted it correctly.");

            return ERROR;
        }

        if (AkmcCore.USER_KEYS.unbindKey(username, key)) {
            reply(context, "Key was successfully unbound!", ChatFormatting.GREEN);

            return SUCCESS;
        } else if (AkmcCore.USER_KEYS.getUsers().contains(username)) {
            fail(context, "The user has no such key.");

            return ERROR;
        } else {
            fail(context, "No such user on record.");

            return ERROR;
        }
    }

    private static int playerInfo(CommandContext<CommandSourceStack> context, String username) {
        List<UserKeys.UserKey> keys = AkmcCore.USER_KEYS.getUserKeys(username);

        if (keys == null || keys.isEmpty()) {
            fail(context, "No such user on record.");

            return ERROR;
        }

        MutableComponent message = Component.empty();
        message.append(Component.literal(username).withStyle(ChatFormatting.YELLOW));
        message.append(" has ");
        message.append(Component.literal(String.valueOf(keys.size())).withStyle(ChatFormatting.AQUA));
        if (keys.size() == 1) {
            message.append(" key bound:");
        } else {
            message.append(" keys bound:");
        }

        int i = 1;
        for (UserKeys.UserKey key : keys) {
            message.append("\n  %s. ".formatted(i));

            String keyString = key.key.toString();
            message.append(Component.literal(keyString)
                    .withStyle(ChatFormatting.GOLD)
                    .withStyle(Style.EMPTY
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy.")))
                            .withClickEvent(new ClickEvent.CopyToClipboard(keyString))));

            if (key.issuingPlayer != null) {
                message.append("\n    └ Issued by: ");
                message.append(Component.literal(key.issuingPlayer).withStyle(ChatFormatting.YELLOW));
            } else {
                message.append("\n    └ Issued via server console.");
            }

            message.append("\n    └ Added at: ");
            message.append(Component.literal(
                            DateTimeFormatter.RFC_1123_DATE_TIME.format(key.registrationTime.atOffset(ZoneOffset.UTC)))
                    .withStyle(ChatFormatting.GRAY));

            i++;
        }

        reply(context, message);

        return SUCCESS;
    }

    private static boolean admin(CommandSourceStack source) {
        return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS));
    }

    private static void reply(
            CommandContext<CommandSourceStack> context, String message, ChatFormatting... formatting) {
        reply(context, Component.literal(message).withStyle(formatting));
    }

    private static void reply(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSystemMessage(message);
    }

    private static void fail(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendFailure(Component.literal(message));
    }
}
