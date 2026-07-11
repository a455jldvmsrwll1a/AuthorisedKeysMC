package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Users;

public sealed interface PublicKeysSuggestions extends SuggestionProvider<CommandSourceStack> {
    final class Self implements PublicKeysSuggestions {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(
                CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            if (!(context.getSource().getSender() instanceof Player player)) {
                return builder.buildFuture();
            }

            List<Users.UserKey> keys = AkmcCore.USERS.getUserKeys(player.getName());
            if (keys == null || keys.isEmpty()) {
                return builder.buildFuture();
            }

            keys.forEach(key -> builder.suggest(key.key().toString()));

            return builder.buildFuture();
        }
    }

    final class ByUsername implements PublicKeysSuggestions {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(
                CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            String username = StringArgumentType.getString(context, "username");

            List<Users.UserKey> keys = AkmcCore.USERS.getUserKeys(username);

            if (keys == null || keys.isEmpty()) {
                return builder.buildFuture();
            }

            keys.forEach(key -> builder.suggest(key.key().toString()));

            return builder.buildFuture();
        }
    }
}
