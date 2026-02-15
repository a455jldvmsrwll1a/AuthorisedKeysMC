package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.UserKeys;

public final class SelfKeysSuggestions implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayer();

        if (player == null) {
            return builder.buildFuture();
        }

        List<UserKeys.UserKey> keys = AuthorisedKeysModCore.USER_KEYS.getUserKeys(player.getUUID());
        if (keys == null) {
            return builder.buildFuture();
        }

        keys.forEach(key -> builder.suggest(key.key.toString()));

        return builder.buildFuture();
    }
}
