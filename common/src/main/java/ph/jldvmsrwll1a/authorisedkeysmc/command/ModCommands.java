package ph.jldvmsrwll1a.authorisedkeysmc.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.UserKeys;
import ph.jldvmsrwll1a.authorisedkeysmc.UsernameAliases;
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
                .then(literal("list").requires(ModCommands::admin).executes(ModCommands::listUsers))
                .then(literal("info")
                        .executes(ModCommands::selfInfo)
                        .then(argument("username", StringArgumentType.word())
                                .requires(ModCommands::admin)
                                .suggests(new UsernameSuggestions())
                                .executes(ModCommands::usernameInfo)))
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
                                                .executes(ModCommands::usernameUnbind)))))
                .then(literal("alias")
                        .requires(ModCommands::admin)
                        .then(literal("list").executes(ModCommands::listAliases))
                        .then(literal("info")
                                .then(argument("original username", StringArgumentType.word())
                                        .suggests(new AliasedUsernameSuggestions())
                                        .executes(ModCommands::aliasInfo)))
                        .then(literal("link")
                                .then(argument("original username", StringArgumentType.word())
                                        .suggests(new UsernameSuggestions())
                                        .then(argument("replacement uuid", UuidArgument.uuid())
                                                .suggests(new KnownUuidSuggestions())
                                                .executes(ModCommands::link)
                                                .then(argument("reason", StringArgumentType.greedyString())
                                                        .executes(ModCommands::link)))))
                        .then(literal("unlink")
                                .then(argument("original username", StringArgumentType.word())
                                        .suggests(new AliasedUsernameSuggestions())
                                        .executes(ModCommands::unlink)))));
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
            Constants.LOG.error("Could not run reload command: {}", e.toString());
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

    private static int listUsers(CommandContext<CommandSourceStack> context) {
        Set<String> names = AkmcCore.USER_KEYS.getUsers();
        int len = names.size();

        MutableComponent message = Component.empty();
        if (len == 1) {
            message.append("There is ");
        } else {
            message.append("There are ");
        }
        message.append(Component.literal(String.valueOf(len)).withStyle(ChatFormatting.AQUA));
        if (len == 1) {
            message.append(" user on record:");
        } else {
            message.append(" users on record:");
        }

        int i = 0;
        for (String name : names) {
            i++;

            List<UserKeys.UserKey> keys = AkmcCore.USER_KEYS.getUserKeys(name);
            if (keys == null || keys.isEmpty()) {
                continue;
            }

            message.append("\n  %s. ".formatted(i));
            message.append(Component.literal(name)
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.YELLOW)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.SuggestCommand("/akmc user %s".formatted(name)))));
            message.append(" (");
            message.append(Component.literal(String.valueOf(keys.size())).withStyle(ChatFormatting.AQUA));
            if (keys.size() == 1) {
                message.append(" key)");
            } else {
                message.append(" keys)");
            }
        }

        reply(context, message);

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

        switch (AkmcCore.USER_KEYS.bindKey(player.getPlainTextName(), player.getPlainTextName(), key)) {
            case SUCCESS -> {
                reply(context, "Bound your key!", ChatFormatting.GREEN);

                return SUCCESS;
            }
            case ALREADY_EXISTS -> {
                fail(context, "You have already bound this key.");

                return ERROR;
            }
            case TOO_MANY -> {
                fail(context, "You cannot bind more than %s keys at a time.".formatted(AkmcCore.CONFIG.maxKeyCount));

                return ERROR;
            }
            default -> throw new IllegalStateException("Invalid bind result.");
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

        switch (AkmcCore.USER_KEYS.unbindKey(player.getPlainTextName(), key, !AkmcCore.CONFIG.registrationRequired)) {
            case SUCCESS -> {
                reply(context, "Unbound your key!", ChatFormatting.GREEN);

                return SUCCESS;
            }
            case NO_SUCH_KEY -> {
                fail(context, "You have no such key.");

                return ERROR;
            }
            case CANNOT_BE_EMPTY -> {
                fail(
                        context,
                        "As registration is required, you cannot unbind all of your keys.\n\nIf you want to move to a new key, bind the new key, and then unbind the old one.");

                return ERROR;
            }
            default -> throw new IllegalStateException("Invalid unbind result.");
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

        switch (AkmcCore.USER_KEYS.bindKey(username, player != null ? player.getPlainTextName() : null, key)) {
            case SUCCESS -> {
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
            }
            case ALREADY_EXISTS -> {
                fail(context, "This user already has that key.");

                return ERROR;
            }
            case TOO_MANY -> {
                fail(
                        context,
                        "Only up to %s keys can be bound to a given username.".formatted(AkmcCore.CONFIG.maxKeyCount));

                return ERROR;
            }
            default -> throw new IllegalStateException("Invalid bind result.");
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

        switch (AkmcCore.USER_KEYS.unbindKey(username, key, true)) {
            case SUCCESS -> {
                reply(context, "Key was successfully unbound!", ChatFormatting.GREEN);

                ServerPlayer player = context.getSource().getPlayer();
                ServerPlayer targetPlayer =
                        context.getSource().getServer().getPlayerList().getPlayer(username);

                if (targetPlayer != null) {
                    String keyString = key.toString();

                    if (player != null) {
                        targetPlayer.sendSystemMessage(Component.empty()
                                .append(player.getDisplayName())
                                .append(" unbound one of your keys: ")
                                .append(Component.literal(keyString)
                                        .withStyle(Style.EMPTY
                                                .withColor(ChatFormatting.GOLD)
                                                .withHoverEvent(
                                                        new HoverEvent.ShowText(Component.literal("Click to copy.")))
                                                .withClickEvent(new ClickEvent.CopyToClipboard(keyString)))));
                    } else {
                        targetPlayer.sendSystemMessage(Component.literal("One of your keys was unbound: ")
                                .append(Component.literal(keyString)
                                        .withStyle(Style.EMPTY
                                                .withColor(ChatFormatting.GOLD)
                                                .withHoverEvent(
                                                        new HoverEvent.ShowText(Component.literal("Click to copy.")))
                                                .withClickEvent(new ClickEvent.CopyToClipboard(keyString)))));
                    }
                }

                return SUCCESS;
            }
            case NO_SUCH_KEY -> {
                fail(context, "The user has no such key.");

                return ERROR;
            }
            case NO_SUCH_USER -> {
                fail(context, "No such user on record.");

                return ERROR;
            }
            default -> throw new IllegalStateException("Invalid unbind result.");
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

    private static int listAliases(CommandContext<CommandSourceStack> context) {
        Set<String> names = AkmcCore.USER_ALIASES.getAliasedUsernames();
        int len = names.size();

        MutableComponent message = Component.empty();
        if (len == 1) {
            message.append("There is ");
        } else {
            message.append("There are ");
        }
        message.append(Component.literal(String.valueOf(len)).withStyle(ChatFormatting.AQUA));
        if (len == 1) {
            message.append(" alias rule:");
        } else {
            message.append(" alias rules:");
        }

        int i = 1;
        for (String name : names) {
            message.append("\n  %s. ".formatted(i));
            message.append(Component.literal(name)
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.YELLOW)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.SuggestCommand("/akmc alias info %s".formatted(name)))));

            i++;
        }

        reply(context, message);

        return SUCCESS;
    }

    private static int aliasInfo(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "original username");

        Optional<UsernameAliases.Alias> maybeAlias = AkmcCore.USER_ALIASES.getAlias(username);
        if (maybeAlias.isEmpty()) {
            fail(context, "No such alias exists.");

            return ERROR;
        }

        UsernameAliases.Alias alias = maybeAlias.get();
        String idStr = alias.id().toString();

        MutableComponent message = Component.empty()
                .append("The username ")
                .append(Component.literal(username).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" maps to the UUID:\n"))
                .append(Component.literal(idStr)
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.GOLD)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy UUID.")))
                                .withClickEvent(new ClickEvent.CopyToClipboard(idStr))));

        if (alias.issuer() != null) {
            message.append("\n └ Issued by: ");
            message.append(Component.literal(alias.issuer()).withStyle(ChatFormatting.YELLOW));
        } else {
            message.append("\n └ Issued via server console.");
        }

        message.append("\n └ Added at: ");
        message.append(Component.literal(DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        alias.creationTime().atOffset(ZoneOffset.UTC)))
                .withStyle(ChatFormatting.GRAY));

        if (alias.reason() != null) {
            message.append("\n └ Reason: ");
            message.append(Component.literal(alias.reason()).withStyle(ChatFormatting.GREEN));
        }

        reply(context, message);

        return SUCCESS;
    }

    private static int link(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "original username");
        UUID id = UuidArgument.getUuid(context, "replacement uuid");

        String reason;
        try {
            reason = StringArgumentType.getString(context, "reason");
        } catch (IllegalArgumentException ignored) {
            reason = null;
        }

        ServerPlayer issuer = context.getSource().getPlayer();

        boolean wasAdded = AkmcCore.USER_ALIASES.link(
                username,
                id,
                issuer != null ? issuer.getPlainTextName() : null,
                (reason != null && !reason.isBlank()) ? reason : null);

        if (!wasAdded) {
            fail(context, "There is already an alias rule that targets the username %s!".formatted(username));

            return ERROR;
        }

        reply(context, "Successfully linked!", ChatFormatting.GREEN);

        PlayerList players = context.getSource().getServer().getPlayerList();
        NameAndId linkedProfile = new NameAndId(id, username);

        if (players.isOp(linkedProfile)) {
            reply(context, "Caution: the linked profile has operator privileges!", ChatFormatting.GOLD);
        }

        if (players.isUsingWhitelist() && !players.isWhiteListed(linkedProfile)) {
            reply(context, "Note: the whitelist currently prevents the user from joining.");
        }

        if (players.getBans().isBanned(linkedProfile)) {
            reply(context, "Note: the linked profile cannot join because they are banned.");
        }

        // Warn affected player(s) currently in the server.
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player.getUUID().equals(id)) {
                player.sendSystemMessage(
                        Component.empty()
                                .append(Component.literal("Warning: ")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                .append("the username ")
                                .append(Component.literal(username).withStyle(ChatFormatting.YELLOW))
                                .append(
                                        " has been linked to your current player ID.\n\nFor changes to take effect, reconnect with that username."));
            } else if (player.getPlainTextName().equals(username)) {
                player.sendSystemMessage(
                        Component.empty()
                                .append(Component.literal("Warning: ")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                .append(
                                        "Your username has been linked to another ID.\n\nBy reconnecting, you will lose access to your current player data, and will instead access the player data of the linked ID."));
            }
        });

        return SUCCESS;
    }

    private static int unlink(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "original username");

        Optional<UUID> oldId = AkmcCore.USER_ALIASES.unlink(username);

        if (oldId.isEmpty()) {
            fail(context, "No such alias rule.");

            return ERROR;
        }

        UUID id = oldId.get();
        String idStr = id.toString();
        reply(
                context,
                Component.empty()
                        .append(Component.literal("Successfully unlinked!\n").withStyle(ChatFormatting.GREEN))
                        .append("The linked ID was ")
                        .append(Component.literal(idStr)
                                .withStyle(Style.EMPTY
                                        .withColor(ChatFormatting.GOLD)
                                        .withHoverEvent(
                                                new HoverEvent.ShowText(Component.literal("Click to copy UUID.")))
                                        .withClickEvent(new ClickEvent.CopyToClipboard(idStr)))));

        // Warn affected player(s) currently in the server.
        ServerPlayer affectedPlayer =
                context.getSource().getServer().getPlayerList().getPlayer(id);
        if (affectedPlayer != null && affectedPlayer.getPlainTextName().equals(username)) {
            affectedPlayer.sendSystemMessage(
                    Component.empty()
                            .append(Component.literal("Warning: ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .append(
                                    "Your username has been unlinked from your current ID.\n\nBy reconnecting, you will lose access to your current player data, and will instead access the player data of your original ID."));
        }

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
