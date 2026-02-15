package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;

public final class RegisteredUserIdSuggestions implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Enumeration<UUID> users = AuthorisedKeysModCore.USER_KEYS.getUsers();
        while (users.hasMoreElements()) {
            UUID id = users.nextElement();

            if (AuthorisedKeysModCore.USER_KEYS.userHasAnyKeys(id)) {
                builder.suggest(id.toString());
            }
        }

        return builder.buildFuture();
    }
}
