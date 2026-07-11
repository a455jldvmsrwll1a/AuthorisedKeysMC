package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.concurrent.CompletableFuture;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

public final class KnownUuidSuggestions implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Server server = context.getSource().getSender().getServer();

        for (OfflinePlayer player : server.getOfflinePlayers()) {
            builder.suggest(player.getUniqueId().toString());
        }

        return builder.buildFuture();
    }
}
