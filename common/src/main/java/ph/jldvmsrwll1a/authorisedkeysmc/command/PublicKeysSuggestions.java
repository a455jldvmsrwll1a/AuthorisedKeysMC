package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.UserKeys;

public sealed interface PublicKeysSuggestions extends SuggestionProvider<CommandSourceStack> {
    final class Self implements PublicKeysSuggestions {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(
                CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            ServerPlayer player = context.getSource().getPlayer();

            if (player == null) {
                return builder.buildFuture();
            }

            List<UserKeys.UserKey> keys = AkmcCore.USER_KEYS.getUserKeys(player.getPlainTextName());
            if (keys == null || keys.isEmpty()) {
                return builder.buildFuture();
            }

            keys.forEach(key -> builder.suggest(key.key.toString()));

            return builder.buildFuture();
        }
    }

    final class ByUsername implements PublicKeysSuggestions {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(
                CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            String username = StringArgumentType.getString(context, "username");

            List<UserKeys.UserKey> keys = AkmcCore.USER_KEYS.getUserKeys(username);

            if (keys == null || keys.isEmpty()) {
                return builder.buildFuture();
            }

            keys.forEach(key -> builder.suggest(key.key.toString()));

            return builder.buildFuture();
        }
    }
}
