package ph.jldvmsrwll1a.authorisedkeysmc.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
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
                .then(literal("unbind")
                        .then(argument("key", StringArgumentType.string())
                                .suggests(new SelfKeysSuggestions())
                                .executes(ModCommands::unbind))));
    }

    private static int hello(CommandContext<CommandSourceStack> context) {
        reply(
                context,
                Component.empty()
                        .append(Component.literal("== AuthorisedKeysMC ==")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                        .append(Component.literal("\n\nStatus: "))
                        .append(Component.literal("ENFORCING").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));

        return SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        AuthorisedKeysModCore.reload();
        reply(context, "Reloaded!");

        return SUCCESS;
    }

    private static int enable(CommandContext<CommandSourceStack> context) {
        reply(
                context,
                Component.literal("AKMC is now ")
                        .append(Component.literal("ENFORCING").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));

        return SUCCESS;
    }

    private static int disable(CommandContext<CommandSourceStack> context) {
        reply(
                context,
                Component.literal("AKMC is now ")
                        .append(Component.literal("ON STANDBY").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));

        return SUCCESS;
    }

    private static int bind(CommandContext<CommandSourceStack> context) {

        return SUCCESS;
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
