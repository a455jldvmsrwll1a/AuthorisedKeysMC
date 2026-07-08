package ph.jldvmsrwll1a.authorisedkeysmc;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.util.InstantTypeAdapter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.util.WriteUtil;

@NullMarked
public class Users {
    private static final GsonBuilder GSON_BUILDER = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(AkPublicKey.class, new AkPublicKeyTypeAdapter())
            .setPrettyPrinting();

    private static final Object WRITE_LOCK = new Object();

    private HashMap<String, User> users = new HashMap<>();
    private HashSet<String> aliasedUsernames = new HashSet<>();

    public synchronized boolean userHasData(String username) {
        User user = users.get(username);
        return user != null && !user.isEmpty();
    }

    public synchronized boolean userHasAnyKeys(String username) {
        User user = users.get(username);
        if (user == null) {
            return false;
        }

        return !user.keys.isEmpty();
    }

    public synchronized boolean userHasKey(String username, AkPublicKey key) {
        User user = users.get(username);
        if (user == null) {
            return false;
        }

        return user.keys.stream().anyMatch(userKey -> AkPublicKey.nullableEqual(userKey.key, key));
    }

    public synchronized @Nullable List<UserKey> getUserKeys(String username) {
        User user = users.get(username);
        if (user == null) {
            return null;
        }

        return user.keys;
    }

    public synchronized Optional<Alias> getUserAlias(String username) {
        User user = users.get(username);
        if (user == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(user.alias);
    }

    public synchronized Set<String> getUsernames() {
        return users.keySet();
    }

    public Iterator<String> getAliasedUsernames() {
        return aliasedUsernames.iterator();
    }

    public synchronized boolean linkAlias(
            String username, UUID replacementId, @Nullable String issuer, @Nullable String reason) {
        User user = users.computeIfAbsent(username, k -> new User());

        if (user.alias != null) {
            return false;
        }

        user.alias = new Alias(replacementId, Instant.now(), issuer, reason);
        write();
        aliasedUsernames.add(username);

        Constants.LOG.info(
                "AKMC: New alias created for user \"{}\" to ID {}. Issuer: {}; Reason: {}",
                username,
                replacementId,
                issuer != null ? issuer : "<server>",
                reason != null ? reason : "<none>");

        return true;
    }

    public synchronized Optional<UUID> unlinkAlias(String username) {
        User user = users.get(username);

        if (user == null || user.alias == null) {
            return Optional.empty();
        }

        UUID id = user.alias.id;
        user.alias = null;

        if (user.isEmpty()) {
            users.remove(username);
        }

        write();
        aliasedUsernames.remove(username);

        Constants.LOG.info("AKMC: Alias for user \"{}\" was removed. (was linked to ID {})", username, id);

        return Optional.of(id);
    }

    public synchronized BindPublicKeyResult bindKey(String username, @Nullable String issuer, AkPublicKey key) {
        User user = users.computeIfAbsent(username, k -> new User());

        if (user.keys.size() >= AkmcCore.CONFIG.maxKeyCount) {
            return BindPublicKeyResult.TOO_MANY;
        }

        if (user.keys.stream().anyMatch(entry -> entry.key.equals(key))) {
            return BindPublicKeyResult.ALREADY_EXISTS;
        }

        user.keys.add(new UserKey(key, issuer, Instant.now()));

        if (issuer == null) {
            Constants.LOG.info("Key {} has been bound to {}.", key, username);
        } else {
            Constants.LOG.info("Key {} has been bound by {} to {}.", key, issuer, username);
        }

        write();

        return BindPublicKeyResult.SUCCESS;
    }

    public synchronized UnbindPublicKeyResult unbindKey(String username, AkPublicKey key, boolean allowEmpty) {
        User user = users.get(username);

        if (user == null) {
            return UnbindPublicKeyResult.NO_SUCH_USER;
        }

        if (!allowEmpty && user.keys.size() == 1) {
            return UnbindPublicKeyResult.CANNOT_BE_EMPTY;
        }

        if (!user.keys.removeIf(userKey -> userKey.key.equals(key))) {
            return UnbindPublicKeyResult.NO_SUCH_KEY;
        }

        if (user.isEmpty()) {
            users.remove(username);
        }

        Constants.LOG.info("AKMC: The public key {} has been unbound from {}.", key, username);

        write();

        return UnbindPublicKeyResult.SUCCESS;
    }

    public void read() {
        HashMap<String, User> newMap = new HashMap<>();
        HashSet<String> newAliasedUsernames = new HashSet<>();
        List<UserJsonEntry> entries;

        try {
            String json;
            synchronized (WRITE_LOCK) {
                json = Files.readString(AkmcCore.FILE_PATHS.USERS_JSON_PATH);
            }

            entries = GSON_BUILDER.create().fromJson(json, new TypeToken<List<UserJsonEntry>>() {}.getType());
        } catch (FileNotFoundException | NoSuchFileException ignored) {
            // Default to an empty map.
            synchronized (this) {
                users = new HashMap<>();
            }

            return;
        } catch (JsonSyntaxException | IOException e) {
            Constants.LOG.error("Could not load user entries list: {}", e.toString());

            throw new RuntimeException("Failed to read user keys.", e);
        }

        // Keep track of usernames with ID aliases.
        entries.forEach(entry -> {
            User user = new User();
            user.alias = entry.alias;
            user.keys = new ArrayList<>(entry.keys);

            if (!user.isEmpty()) {
                newMap.put(entry.user, user);

                if (user.alias != null) {
                    newAliasedUsernames.add(entry.user);
                }
            }
        });

        synchronized (this) {
            users = newMap;
            aliasedUsernames = newAliasedUsernames;
        }

        Constants.LOG.debug("Read {} user entries from disk.", users.size());
    }

    public void write() {
        List<UserJsonEntry> out = new ArrayList<>();
        synchronized (this) {
            for (Map.Entry<String, User> entry : users.entrySet()) {
                User user = entry.getValue();

                if (!user.isEmpty()) {
                    out.add(new UserJsonEntry(entry.getKey(), user.alias, user.keys));
                }
            }
        }

        String json = GSON_BUILDER.create().toJson(out);

        try {
            synchronized (WRITE_LOCK) {
                Files.createDirectories(AkmcCore.FILE_PATHS.MOD_DIR);
                WriteUtil.writeString(AkmcCore.FILE_PATHS.USERS_JSON_PATH, json);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} user entries to disk.", users.size());
    }

    public enum BindPublicKeyResult {
        SUCCESS,
        ALREADY_EXISTS,
        TOO_MANY
    }

    public enum UnbindPublicKeyResult {
        SUCCESS,
        NO_SUCH_KEY,
        NO_SUCH_USER,
        CANNOT_BE_EMPTY,
    }

    public record Alias(
            @SerializedName("replacement_id") UUID id,
            @SerializedName("time_added") Instant creationTime,
            @SerializedName("issued_by") @Nullable String issuer,
            @SerializedName("reason") @Nullable String reason) {}

    public record UserKey(
            @SerializedName("key") AkPublicKey key,
            @SerializedName("issued_by") @Nullable String issuingPlayer,
            @SerializedName("time_added") Instant registrationTime) {}

    private static final class User {
        public @Nullable Alias alias;
        public ArrayList<UserKey> keys = new ArrayList<>();

        /// Whether this User is empty and can be removed from the database.
        public boolean isEmpty() {
            return alias == null && keys.isEmpty();
        }
    }

    private record UserJsonEntry(
            @SerializedName("user") String user,
            @SerializedName("alias") @Nullable Alias alias,
            @SerializedName("keys") ArrayList<UserKey> keys) {}

    private static final class AkPublicKeyTypeAdapter extends TypeAdapter<AkPublicKey> {

        @Override
        public void write(JsonWriter out, AkPublicKey value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public AkPublicKey read(JsonReader in) throws IOException {
            return new AkPublicKey(in.nextString());
        }
    }
}
