package ph.jldvmsrwll1a.authorisedkeysmc;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.util.InstantTypeAdapter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;

public class UserKeys {
    private static final Object WRITE_LOCK = new Object();

    private HashMap<UUID, ArrayList<UserKey>> userKeysMap = new HashMap<>();

    public synchronized boolean userHasAnyKeys(UUID playerId) {
        List<UserKey> userKeys = userKeysMap.get(playerId);
        if (userKeys == null) {
            return false;
        }

        return !userKeys.isEmpty();
    }

    public synchronized boolean userHasKey(UUID playerId, AkPublicKey key) {
        List<UserKey> userKeys = userKeysMap.get(playerId);
        if (userKeys == null) {
            return false;
        }

        return userKeys.stream().anyMatch(userKey -> AkPublicKey.nullableEqual(userKey.key, key));
    }

    public synchronized @Nullable List<UserKey> getUserKeys(UUID playerId) {
        return userKeysMap.get(playerId);
    }

    public synchronized Set<UUID> getUsers() {
        return userKeysMap.keySet();
    }

    public synchronized boolean bindKey(UUID playerId, @Nullable UUID issuingPlayerId, AkPublicKey key) {
        ArrayList<UserKey> keys = userKeysMap.computeIfAbsent(playerId, k -> new ArrayList<>(1));

        if (keys.stream().anyMatch(entry -> entry.key.equals(key))) {
            return false;
        }

        UserKey newKey = new UserKey();
        newKey.key = key;
        newKey.issuingPlayer = issuingPlayerId;
        newKey.registrationTime = Instant.now();
        keys.add(newKey);

        if (issuingPlayerId == null) {
            Constants.LOG.info("Key {} has been bound to {}.", Base64Util.encode(key.getEncoded()), playerId);
        } else {
            Constants.LOG.info(
                    "Key {} has been bound by {} to {}.",
                    Base64Util.encode(key.getEncoded()),
                    issuingPlayerId,
                    playerId);
        }

        write();

        return true;
    }

    public synchronized boolean unbindKey(UUID playerId, AkPublicKey key) {
        ArrayList<UserKey> keys = userKeysMap.get(playerId);

        if (keys == null || keys.isEmpty()) {
            return false;
        }

        if (!keys.removeIf(userKey -> userKey.key.equals(key))) {
            return false;
        }

        Constants.LOG.info("AKMC: The public key {} has been unbound from {}.", key, playerId);

        write();

        return true;
    }

    public void read() {
        HashMap<UUID, ArrayList<UserKey>> newMap = new HashMap<>();
        List<UserJsonEntry> entries;

        try {
            String json;
            synchronized (WRITE_LOCK) {
                json = Files.readString(AuthorisedKeysModCore.FILE_PATHS.AUTHORISED_KEYS_PATH);
            }

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                    .create();
            entries = gson.fromJson(json, new TypeToken<List<UserJsonEntry>>() {}.getType());
        } catch (FileNotFoundException ignored) {
            // Default to an empty map.
            synchronized (this) {
                userKeysMap = new HashMap<>();
            }

            return;
        } catch (JsonSyntaxException | IOException e) {
            Constants.LOG.error("Could not load user entries list: {}", e.toString());

            throw new RuntimeException("Failed to read user keys.", e);
        }

        entries.forEach(entry -> {
            List<UserKey> keys = entry.keys.stream()
                    .map(jsonEntry -> {
                        UserKey userKey = new UserKey();
                        userKey.key = new AkPublicKey(jsonEntry.key);
                        userKey.issuingPlayer = jsonEntry.issued_by;
                        userKey.registrationTime = jsonEntry.time_added;

                        return userKey;
                    })
                    .toList();

            newMap.put(entry.user, new ArrayList<>(keys));
        });

        synchronized (this) {
            userKeysMap = newMap;
        }

        Constants.LOG.debug("Read {} user entries from disk.", userKeysMap.size());
    }

    public void write() {
        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.MOD_DIR);

            List<UserJsonEntry> out = new ArrayList<>();
            synchronized (this) {
                for (var entry : userKeysMap.entrySet()) {
                    List<UserKey> keys = entry.getValue();

                    out.add(new UserJsonEntry(
                            entry.getKey(),
                            keys.stream()
                                    .map(key -> new UserKeyJsonEntry(
                                            Base64Util.encode(key.key.getEncoded()),
                                            key.issuingPlayer,
                                            key.registrationTime))
                                    .toList()));
                }
            }

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                    .setPrettyPrinting()
                    .create();

            synchronized (WRITE_LOCK) {
                Files.writeString(AuthorisedKeysModCore.FILE_PATHS.AUTHORISED_KEYS_PATH, gson.toJson(out));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} user entries to disk.", userKeysMap.size());
    }

    public static class UserKey {
        public AkPublicKey key;
        public @Nullable UUID issuingPlayer;
        public Instant registrationTime;
    }

    private record UserJsonEntry(UUID user, List<UserKeyJsonEntry> keys) {}

    private record UserKeyJsonEntry(String key, @Nullable UUID issued_by, Instant time_added) {}
}
