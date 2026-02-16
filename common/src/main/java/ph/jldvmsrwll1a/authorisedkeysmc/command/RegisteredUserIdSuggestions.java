package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;

public final class RegisteredUserIdSuggestions implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        AuthorisedKeysModCore.USER_KEYS.getUsers().forEach(id -> {
            if (AuthorisedKeysModCore.USER_KEYS.userHasAnyKeys(id)) {
                builder.suggest(id.toString());
            }
        });

        return builder.buildFuture();
    }
}
