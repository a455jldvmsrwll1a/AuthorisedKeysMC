package ph.jldvmsrwll1a.authorisedkeysmc;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.util.InstantTypeAdapter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.*;
import net.minecraft.core.UUIDUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.util.WriteUtil;

@NullMarked
public final class UsernameAliases {
    private static final Object WRITE_LOCK = new Object();

    private HashMap<String, Alias> aliases = new HashMap<>();

    // TODO: we probably just want to directly use getAlias(). that way we control how we log stuff during the login

    public synchronized GameProfile resolve(GameProfile original) {
        return getAlias(original.name())
                .map(alias -> new GameProfile(alias.id, original.name(), original.properties()))
                .orElse(original);
    }

    public synchronized GameProfile resolveOffline(String username) {
        return getAlias(username)
                .map(alias -> new GameProfile(alias.id, username))
                .orElse(UUIDUtil.createOfflineProfile(username));
    }

    public synchronized Optional<Alias> getAlias(String username) {
        return Optional.ofNullable(aliases.get(username));
    }

    public synchronized Set<String> getAliasedUsernames() {
        return aliases.keySet();
    }

    public synchronized boolean link(
            String username, UUID replacementId, @Nullable String issuer, @Nullable String reason) {
        if (aliases.containsKey(username)) {
            return false;
        }

        aliases.put(username, new Alias(replacementId, Instant.now(), issuer, reason));
        write();

        Constants.LOG.info(
                "AKMC: New alias created for user \"{}\" to ID {}. Issuer: {}; Reason: {}",
                username,
                replacementId,
                issuer != null ? issuer : "<server>",
                reason != null ? reason : "<none>");

        return true;
    }

    public synchronized boolean unlink(String username) {
        if (!aliases.containsKey(username)) {
            return false;
        }

        aliases.remove(username);
        write();

        Constants.LOG.info("AKMC: Alias for user \"{}\" was removed.", username);

        return true;
    }

    public void read() {
        HashMap<String, Alias> newMap = new HashMap<>();
        List<AliasJsonEntry> entries;

        try {
            String json;
            synchronized (WRITE_LOCK) {
                json = Files.readString(AkmcCore.FILE_PATHS.ALIASES_PATH);
            }

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                    .create();
            entries = gson.fromJson(json, new TypeToken<List<AliasJsonEntry>>() {}.getType());
        } catch (FileNotFoundException | NoSuchFileException ignored) {
            // Default to an empty map.
            synchronized (this) {
                aliases = new HashMap<>();
            }

            return;
        } catch (JsonSyntaxException | IOException e) {
            Constants.LOG.error("Could not load alias list: {}", e.toString());

            throw new RuntimeException("Failed to read aliases.", e);
        }

        entries.forEach(entry -> {
            Alias alias = new Alias(entry.replacement_id, entry.time_added, entry.issued_by, entry.reason);

            newMap.put(entry.target_name, alias);
        });

        synchronized (this) {
            aliases = newMap;
        }

        Constants.LOG.debug("AKMC: read {} alias entries from disk.", aliases.size());
    }

    public void write() {
        ArrayList<AliasJsonEntry> out = new ArrayList<>();
        synchronized (this) {
            for (var entry : aliases.entrySet()) {
                Alias alias = entry.getValue();

                out.add(new AliasJsonEntry(entry.getKey(), alias.id, alias.creationTime, alias.issuer, alias.reason));
            }
        }

        String json = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .setPrettyPrinting()
                .create()
                .toJson(out);

        try {
            synchronized (WRITE_LOCK) {
                Files.createDirectories(AkmcCore.FILE_PATHS.MOD_DIR);
                WriteUtil.writeString(AkmcCore.FILE_PATHS.ALIASES_PATH, json);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("AKMC: wrote {} aliases to disk.", aliases.size());
    }

    public record Alias(
            UUID id,
            Instant creationTime,
            @Nullable String issuer,
            @Nullable String reason) {}

    private record AliasJsonEntry(
            String target_name,
            UUID replacement_id,
            Instant time_added,
            @Nullable String issued_by,
            @Nullable String reason) {}
}
