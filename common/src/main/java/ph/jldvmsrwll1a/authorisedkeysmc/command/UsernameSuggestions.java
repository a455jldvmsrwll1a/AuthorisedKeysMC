package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;

public final class UsernameSuggestions implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Set<String> usernames = new HashSet<>(AkmcCore.USER_KEYS.getUsers());

        context.getSource()
                .getServer()
                .getPlayerList()
                .getPlayers()
                .forEach(player -> usernames.add(player.getPlainTextName()));

        usernames.forEach(username -> {
            if (!AkmcCore.USER_KEYS.userHasAnyKeys(username)) {
                return;
            }

            builder.suggest(username);
        });

        return builder.buildFuture();
    }
}
