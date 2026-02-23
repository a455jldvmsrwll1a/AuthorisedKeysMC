package ph.jldvmsrwll1a.authorisedkeysmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.storage.LevelResource;

public final class KnownUuidSuggestions implements SuggestionProvider<CommandSourceStack> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Path playerDataDir = context.getSource().getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);

        HashSet<UUID> ids = new HashSet<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(playerDataDir)) {
            for (Path file : stream) {
                String baseName = getBaseName(file);

                try {
                    ids.add(UUID.fromString(baseName));
                } catch (IllegalArgumentException ignored) {
                    // Silently ignore.
                }
            }
        } catch (IOException e) {
            builder.suggest("failed_to_read_playerdata_dir");
        }

        ids.forEach(id -> builder.suggest(id.toString()));

        return builder.buildFuture();
    }

    private static String getBaseName(Path path) {
        String fullName = path.getFileName().toString();

        int separatorIndex = fullName.lastIndexOf('.');

        if (separatorIndex > 0) {
            return fullName.substring(0, separatorIndex);
        } else {
            return fullName;
        }
    }
}
