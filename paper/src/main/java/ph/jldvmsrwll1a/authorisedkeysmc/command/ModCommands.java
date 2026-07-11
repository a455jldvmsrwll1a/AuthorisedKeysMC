package ph.jldvmsrwll1a.authorisedkeysmc.command;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static org.bukkit.Server.BROADCAST_CHANNEL_ADMINISTRATIVE;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.Users;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;

public final class ModCommands {
    public static final int SUCCESS = 1;
    public static final int ERROR = -1;

    public static final URI MOD_URL;
    public static final URI DEV_URL;

    private ModCommands() {}

    public static void register(Commands commands) {
        commands.register(literal("akmc")
                .executes(ModCommands::hello)
                .then(literal("status").executes(ModCommands::status))
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
                        .then(argument("public key", StringArgumentType.word()).executes(ModCommands::selfBind)))
                .then(literal("unbind")
                        .then(argument("public key", StringArgumentType.word())
                                .suggests((SuggestionProvider<CommandSourceStack>) new PublicKeysSuggestions.Self())
                                .executes(ModCommands::selfUnbind)))
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
                                                .suggests((SuggestionProvider<CommandSourceStack>)
                                                        new PublicKeysSuggestions.ByUsername())
                                                .executes(ModCommands::usernameUnbind)))
                                .then(literal("alias")
                                        .then(argument("replacement uuid", ArgumentTypes.uuid())
                                                .suggests(new KnownUuidSuggestions())
                                                .executes(ModCommands::makeAlias)
                                                .then(argument("reason", StringArgumentType.greedyString())
                                                        .executes(ModCommands::makeAlias))))
                                .then(literal("unalias").executes(ModCommands::removeAlias))))
                .build());
    }

    public static int hello(CommandContext<CommandSourceStack> context) {
        MutableComponent message = Component.empty()
                .append(Component.literal("== AuthorisedKeysMC ==\n")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withBold(true)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click for mod info.")))
                                .withClickEvent(new ClickEvent.OpenUrl(MOD_URL))))
                .append(Component.literal("By a455jldvmsrwll1a.")
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click for developer info.")))
                                .withClickEvent(new ClickEvent.OpenUrl(DEV_URL))))
                .append(Component.literal("\n\nStatus: "));

        if (AkmcCore.CONFIG.enforcing) {
            message.append(Component.literal("ENFORCING").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            message.append(Component.literal("ON STANDBY").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        if (admin(context.getSource())) {
            Set<String> names = AkmcCore.USERS.getUsernames();
            int len = names.size();

            if (len == 1) {
                message.append("\nThere is ");
            } else {
                message.append("\nThere are ");
            }
            message.append(Component.literal(String.valueOf(len)).withStyle(ChatFormatting.AQUA));
            if (len == 1) {
                message.append(" user on record.");
            } else {
                message.append(" users on record.");
            }
        }

        if (context.getSource().getSender() instanceof Player player) {
            String name = player.getName();
            List<Users.UserKey> keys = AkmcCore.USERS.getUserKeys(name);
            int numKeys = keys != null ? keys.size() : 0;

            message.append("\n\n");
            message.append(Component.literal(name)
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.YELLOW)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.SuggestCommand("/akmc info"))));
            message.append(": ");

            if (AkmcCore.USERS.getUserAlias(name).isPresent()) {
                message.append(Component.literal("[ID aliased] ").withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            message.append(Component.literal(String.valueOf(numKeys)).withStyle(ChatFormatting.AQUA));
            if (numKeys == 1) {
                message.append(" key.");
            } else {
                message.append(" keys.");
            }
        }

        message.append(Component.literal("\n\nTo view available commands, run \"/help akmc\"")
                .setStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(new ClickEvent.SuggestCommand("/help akmc"))));

        reply(context, message);

        return SUCCESS;
    }

    public static int status(CommandContext<CommandSourceStack> context) {
        MutableComponent message = Component.empty().append(Component.literal("Status: "));

        if (AkmcCore.CONFIG.enforcing) {
            message.append(Component.literal("ENFORCING").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            message.append(Component.literal("ON STANDBY").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        reply(context, message);

        return SUCCESS;
    }

    public static int reload(CommandContext<CommandSourceStack> context) {
        try {
            AkmcCore.reload();
        } catch (Exception e) {
            Constants.LOG.error("Could not run reload command: {}", e.toString());
            fail(context, "Failed to reload: %s".formatted(e.toString()));

            return ERROR;
        }

        sendSuccess(context, Component.literal("AKMC reloaded!").withStyle(ChatFormatting.GREEN));

        return SUCCESS;
    }

    public static int enable(CommandContext<CommandSourceStack> context) {
        if (AkmcCore.CONFIG.enforcing) {
            fail(context, "AKMC is already enforcing.");

            return ERROR;
        }

        AkmcCore.CONFIG.enforcing = true;
        AkmcCore.CONFIG.write();

        sendSuccess(
                context,
                Component.literal("AKMC is now ")
                        .append(Component.literal("ENFORCING").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));

        return SUCCESS;
    }

    public static int disable(CommandContext<CommandSourceStack> context) {
        if (!AkmcCore.CONFIG.enforcing) {
            fail(context, "AKMC is already on standby.");

            return ERROR;
        }

        AkmcCore.CONFIG.enforcing = false;
        AkmcCore.CONFIG.write();

        sendSuccess(
                context,
                Component.literal("AKMC is now ")
                        .append(Component.literal("ON STANDBY").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));

        return SUCCESS;
    }

    public static int listUsers(CommandContext<CommandSourceStack> context) {
        Set<String> names = AkmcCore.USERS.getUsernames();
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

            List<Users.UserKey> keys = AkmcCore.USERS.getUserKeys(name);
            if (keys == null) {
                continue;
            }

            message.append("\n  %s. ".formatted(i));
            message.append(Component.literal(name)
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.YELLOW)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.SuggestCommand("/akmc user %s".formatted(name)))));
            message.append(": ");

            if (AkmcCore.USERS.getUserAlias(name).isPresent()) {
                message.append(Component.literal("[ID aliased] ").withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            message.append(Component.literal(String.valueOf(keys.size())).withStyle(ChatFormatting.AQUA));
            if (keys.size() == 1) {
                message.append(" key.");
            } else {
                message.append(" keys.");
            }
        }

        reply(context, message);

        return SUCCESS;
    }

    public static int selfBind(CommandContext<CommandSourceStack> context) {
        Player player = getPlayer(context);
        if (player == null) {
            fail(
                    context,
                    "Must be executed by a player! To bind a key to a specific user, use: /akmc user <username> bind key <public key>");

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

        switch (AkmcCore.USERS.bindKey(player.getName(), player.getName(), key)) {
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

    public static int selfUnbind(CommandContext<CommandSourceStack> context) {
        Player player = getPlayer(context);
        if (player == null) {
            fail(
                    context,
                    "Must be executed by a player! To unbind a key from a specific user, use: /akmc user <username> unbind key <public key>");

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

        switch (AkmcCore.USERS.unbindKey(player.getName(), key, !AkmcCore.CONFIG.registrationRequired)) {
            case SUCCESS -> {
                reply(context, "Unbound your key!", ChatFormatting.GREEN);

                return SUCCESS;
            }
            case NO_SUCH_USER, NO_SUCH_KEY -> {
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

    public static int selfInfo(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof Player player) {
            return playerInfo(context, player.getName());
        } else {
            fail(context, "Must be executed by a player! To query a specific user, use: /akmc user <username>");

            return ERROR;
        }
    }

    public static int usernameInfo(CommandContext<CommandSourceStack> context) {
        return playerInfo(context, StringArgumentType.getString(context, "username"));
    }

    public static int usernameBind(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "username");
        String encodedKey = StringArgumentType.getString(context, "public key");

        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            fail(context, "Invalid public key string. Make sure you copy-pasted it correctly.");

            return ERROR;
        }

        Player player = getPlayer(context);

        switch (AkmcCore.USERS.bindKey(username, player != null ? player.getName() : null, key)) {
            case SUCCESS -> {
                reply(context, "Bound this key to %s!".formatted(username), ChatFormatting.GREEN);

                Player targetPlayer =
                        context.getSource().getSender().getServer().getPlayer(username);
                if (targetPlayer != null) {
                    String keyString = key.toString();

                    if (player != null) {
                        sendMsg(
                                targetPlayer,
                                Component.empty()
                                        .append(player.getName())
                                        .append(" bound a new key to your username: ")
                                        .append(Component.literal(keyString)
                                                .withStyle(Style.EMPTY
                                                        .withColor(ChatFormatting.GOLD)
                                                        .withHoverEvent(new HoverEvent.ShowText(
                                                                Component.literal("Click to copy.")))
                                                        .withClickEvent(new ClickEvent.CopyToClipboard(keyString)))));
                    } else {
                        sendMsg(
                                targetPlayer,
                                Component.literal("A new key has been bound to your username: ")
                                        .append(Component.literal(keyString)
                                                .withStyle(Style.EMPTY
                                                        .withColor(ChatFormatting.GOLD)
                                                        .withHoverEvent(new HoverEvent.ShowText(
                                                                Component.literal("Click to copy.")))
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

    public static int usernameUnbind(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "username");
        String encodedKey = StringArgumentType.getString(context, "public key");

        AkPublicKey key;

        try {
            key = new AkPublicKey(encodedKey);
        } catch (IllegalArgumentException e) {
            fail(context, "Invalid public key string. Make sure you copy-pasted it correctly.");

            return ERROR;
        }

        switch (AkmcCore.USERS.unbindKey(username, key, true)) {
            case SUCCESS -> {
                reply(context, "Key was successfully unbound!", ChatFormatting.GREEN);

                Player player = getPlayer(context);
                Player targetPlayer =
                        context.getSource().getSender().getServer().getPlayer(username);

                if (targetPlayer != null) {
                    String keyString = key.toString();

                    if (player != null) {
                        sendMsg(
                                targetPlayer,
                                Component.empty()
                                        .append(player.getName())
                                        .append(" unbound one of your keys: ")
                                        .append(Component.literal(keyString)
                                                .withStyle(Style.EMPTY
                                                        .withColor(ChatFormatting.GOLD)
                                                        .withHoverEvent(new HoverEvent.ShowText(
                                                                Component.literal("Click to copy.")))
                                                        .withClickEvent(new ClickEvent.CopyToClipboard(keyString)))));
                    } else {
                        sendMsg(
                                targetPlayer,
                                Component.literal("One of your keys was unbound: ")
                                        .append(Component.literal(keyString)
                                                .withStyle(Style.EMPTY
                                                        .withColor(ChatFormatting.GOLD)
                                                        .withHoverEvent(new HoverEvent.ShowText(
                                                                Component.literal("Click to copy.")))
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

    public static int playerInfo(CommandContext<CommandSourceStack> context, String username) {
        List<Users.UserKey> keys = AkmcCore.USERS.getUserKeys(username);

        if (!AkmcCore.USERS.userHasData(username)) {
            fail(context, "No such user on record.");
            return ERROR;
        }

        MutableComponent message = Component.empty();
        message.append("Info for username ");
        message.append(Component.literal(username).withStyle(ChatFormatting.YELLOW));
        message.append(":");

        AkmcCore.USERS.getUserAlias(username).ifPresent(alias -> {
            String idStr = alias.id().toString();

            message.append("\n└ Aliased to ID: ");
            message.append(Component.literal(idStr)
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.DARK_AQUA)
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy UUID.")))
                            .withClickEvent(new ClickEvent.CopyToClipboard(idStr))));

            if (alias.issuer() != null) {
                message.append("\n   Issued by: ");
                message.append(Component.literal(alias.issuer()).withStyle(ChatFormatting.YELLOW));
            } else {
                message.append("\n   Issued via server console.");
            }

            message.append("\n   Added at: ");
            message.append(Component.literal(DateTimeFormatter.RFC_1123_DATE_TIME.format(
                            alias.creationTime().atOffset(ZoneOffset.UTC)))
                    .withStyle(ChatFormatting.GRAY));

            if (alias.reason() != null) {
                message.append("\n   Reason: ");
                message.append(Component.literal(alias.reason()).withStyle(ChatFormatting.GREEN));
            }
        });

        if (keys != null && !keys.isEmpty()) {
            message.append("\n└ ");
            message.append(Component.literal(String.valueOf(keys.size())).withStyle(ChatFormatting.AQUA));
            switch (keys.size()) {
                case 0 -> message.append(" keys bound.");
                case 1 -> message.append(" key bound:");
                default -> message.append(" keys bound:");
            }

            int i = 1;
            for (Users.UserKey key : keys) {
                message.append("\n   └ %s. ".formatted(i));

                String keyString = key.key().toString();
                message.append(Component.literal(keyString)
                        .withStyle(ChatFormatting.GOLD)
                        .withStyle(Style.EMPTY
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy.")))
                                .withClickEvent(new ClickEvent.CopyToClipboard(keyString))));

                if (key.issuingPlayer() != null) {
                    message.append("\n      Issued by: ");
                    message.append(Component.literal(key.issuingPlayer()).withStyle(ChatFormatting.YELLOW));
                } else {
                    message.append("\n      Issued via server console.");
                }

                message.append("\n      Added at: ");
                message.append(Component.literal(DateTimeFormatter.RFC_1123_DATE_TIME.format(
                                key.registrationTime().atOffset(ZoneOffset.UTC)))
                        .withStyle(ChatFormatting.GRAY));

                i++;
            }
        } else {
            message.append("\n└ ");
            message.append(Component.literal("No keys bound.").withStyle(ChatFormatting.RED));
        }

        reply(context, message);

        return SUCCESS;
    }

    public static int makeAlias(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "username");
        UUID id = context.getArgument("replacement uuid", UUID.class);

        String reason;
        try {
            reason = StringArgumentType.getString(context, "reason");
        } catch (IllegalArgumentException ignored) {
            reason = null;
        }

        Player issuer = getPlayer(context);

        boolean wasAdded = AkmcCore.USERS.linkAlias(
                username,
                id,
                issuer != null ? issuer.getName() : null,
                (reason != null && !reason.isBlank()) ? reason : null);

        if (!wasAdded) {
            fail(context, "There is already an alias rule that targets the username %s!".formatted(username));

            return ERROR;
        }

        reply(context, "Successfully linked!", ChatFormatting.GREEN);

        Server server = context.getSource().getSender().getServer();

        // FIXME: whether name or ID is compared should depend on mod config match_player_list_by_name.

        if (server.getOperators().stream().anyMatch(offline -> username.equals(offline.getName()))) {
            reply(context, "Caution: the linked profile has operator privileges!", ChatFormatting.GOLD);
        }

        if (server.hasWhitelist()
                && server.getWhitelistedPlayers().stream().noneMatch(offline -> username.equals(offline.getName()))) {
            reply(context, "Note: the whitelist currently prevents the user from joining.");
        }

        if (server.getBannedPlayers().stream().anyMatch(offline -> username.equals(offline.getName()))) {
            reply(context, "Note: the linked profile cannot join because they are banned.");
        }

        // Warn affected player(s) currently in the server.
        context.getSource().getSender().getServer().getOnlinePlayers().forEach(player -> {
            if (player.getUniqueId().equals(id)) {
                sendMsg(
                        player,
                        Component.empty()
                                .append(Component.literal("Warning: ")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                .append("the username ")
                                .append(Component.literal(username).withStyle(ChatFormatting.YELLOW))
                                .append(
                                        " has been linked to your current player ID.\n\nFor changes to take effect, reconnect with that username."));
            } else if (player.getName().equals(username)) {
                sendMsg(
                        player,
                        Component.empty()
                                .append(Component.literal("Warning: ")
                                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                                .append(
                                        "Your username has been linked to another ID.\n\nBy reconnecting, you will lose access to your current player data, and will instead access the player data of the linked ID."));
            }
        });

        return SUCCESS;
    }

    public static int removeAlias(CommandContext<CommandSourceStack> context) {
        String username = StringArgumentType.getString(context, "username");

        Optional<UUID> oldId = AkmcCore.USERS.unlinkAlias(username);

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
        Player affectedPlayer = context.getSource().getSender().getServer().getPlayer(id);
        if (affectedPlayer != null && affectedPlayer.getName().equals(username)) {
            sendMsg(
                    affectedPlayer,
                    Component.empty()
                            .append(Component.literal("Warning: ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .append(
                                    "Your username has been unlinked from your current ID.\n\nBy reconnecting, you will lose access to your current player data, and will instead access the player data of your original ID."));
        }

        return SUCCESS;
    }

    public static boolean admin(CommandSourceStack source) {
        return source.getSender().isOp();
    }

    private static void reply(
            CommandContext<CommandSourceStack> context, String message, ChatFormatting... formatting) {
        reply(context, Component.literal(message).withStyle(formatting));
    }

    private static void reply(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().getSender().sendMessage(PaperAdventure.asAdventure(message));
    }

    private static void sendMsg(Player player, Component message) {
        player.sendMessage(PaperAdventure.asAdventure(message));
    }

    private static void fail(CommandContext<CommandSourceStack> context, String message) {
        reply(context, Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private static Player getPlayer(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof Player player) {
            return player;
        } else {
            return null;
        }
    }

    private static void sendSuccess(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource()
                .getSender()
                .getServer()
                .broadcast(PaperAdventure.asAdventure(message), BROADCAST_CHANNEL_ADMINISTRATIVE);
    }

    static {
        try {
            MOD_URL = new URI("https://github.com/a455jldvmsrwll1a/AuthorisedKeysMC");
            DEV_URL = new URI("https://github.com/a455jldvmsrwll1a");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
