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
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.util.WriteUtil;

public class UserKeys {
    private static final GsonBuilder GSON_BUILDER = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapter(AkPublicKey.class, new AkPublicKeyTypeAdapter())
            .setPrettyPrinting();

    private static final Object WRITE_LOCK = new Object();

    private HashMap<String, User> users = new HashMap<>();

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

    public synchronized Set<String> getUsernames() {
        return users.keySet();
    }

    public synchronized BindResult bindKey(String username, @Nullable String issuer, AkPublicKey key) {
        User user = users.computeIfAbsent(username, k -> new User());

        if (user.keys.size() >= AkmcCore.CONFIG.maxKeyCount) {
            return BindResult.TOO_MANY;
        }

        if (user.keys.stream().anyMatch(entry -> entry.key.equals(key))) {
            return BindResult.ALREADY_EXISTS;
        }

        user.keys.add(new UserKey(key, issuer, Instant.now()));

        if (issuer == null) {
            Constants.LOG.info("Key {} has been bound to {}.", key, username);
        } else {
            Constants.LOG.info("Key {} has been bound by {} to {}.", key, issuer, username);
        }

        write();

        return BindResult.SUCCESS;
    }

    public synchronized UnbindResult unbindKey(String username, AkPublicKey key, boolean allowEmpty) {
        User user = users.get(username);

        if (user == null) {
            return UnbindResult.NO_SUCH_USER;
        }

        if (!allowEmpty && user.keys.size() == 1) {
            return UnbindResult.CANNOT_BE_EMPTY;
        }

        if (!user.keys.removeIf(userKey -> userKey.key.equals(key))) {
            return UnbindResult.NO_SUCH_KEY;
        }

        if (user.keys.isEmpty()) {
            users.remove(username);
        }

        Constants.LOG.info("AKMC: The public key {} has been unbound from {}.", key, username);

        write();

        return UnbindResult.SUCCESS;
    }

    public void read() {
        HashMap<String, User> newMap = new HashMap<>();
        List<UserJsonEntry> entries;

        try {
            String json;
            synchronized (WRITE_LOCK) {
                json = Files.readString(AkmcCore.FILE_PATHS.AUTHORISED_KEYS_PATH);
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

        entries.forEach(entry -> {
            if (entry.keys().isEmpty()) {
                return;
            }

            User user = new User();
            user.keys = new ArrayList<>(entry.keys);

            newMap.put(entry.user, user);
        });

        synchronized (this) {
            users = newMap;
        }

        Constants.LOG.debug("Read {} user entries from disk.", users.size());
    }

    public void write() {
        List<UserJsonEntry> out = new ArrayList<>();
        synchronized (this) {
            for (Map.Entry<String, User> entry : users.entrySet()) {
                User user = entry.getValue();

                if (user.keys.isEmpty()) {
                    continue;
                }

                out.add(new UserJsonEntry(entry.getKey(), user.keys));
            }
        }

        String json = GSON_BUILDER.create().toJson(out);

        try {
            synchronized (WRITE_LOCK) {
                Files.createDirectories(AkmcCore.FILE_PATHS.MOD_DIR);
                WriteUtil.writeString(AkmcCore.FILE_PATHS.AUTHORISED_KEYS_PATH, json);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} user entries to disk.", users.size());
    }

    public enum BindResult {
        SUCCESS,
        ALREADY_EXISTS,
        TOO_MANY
    }

    public enum UnbindResult {
        SUCCESS,
        NO_SUCH_KEY,
        NO_SUCH_USER,
        CANNOT_BE_EMPTY,
    }

    public record UserKey(
            AkPublicKey key,
            @SerializedName("issued_by") @Nullable String issuingPlayer,
            @SerializedName("time_added") Instant registrationTime) {}

    private static final class User {
        public ArrayList<UserKey> keys = new ArrayList<>();
    }

    private record UserJsonEntry(String user, List<UserKey> keys) {}

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
