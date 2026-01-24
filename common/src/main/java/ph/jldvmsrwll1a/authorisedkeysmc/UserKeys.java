package ph.jldvmsrwll1a.authorisedkeysmc;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.util.InstantTypeAdapter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;
import ph.jldvmsrwll1a.authorisedkeysmc.util.KeyUtil;

public class UserKeys {
    private final ConcurrentHashMap<UUID, List<UserKey>> userKeysMap = new ConcurrentHashMap<>();

    public UserKeys() {
        read();
    }

    public boolean userHasAnyKeys(UUID playerId) {
        List<UserKey> userKeys = userKeysMap.get(playerId);
        if (userKeys == null) {
            return false;
        }

        return !userKeys.isEmpty();
    }

    public boolean userHasKey(UUID playerId, Ed25519PublicKeyParameters key) {
        List<UserKey> userKeys = userKeysMap.get(playerId);
        if (userKeys == null) {
            return false;
        }

        return userKeys.stream().anyMatch(userKey -> KeyUtil.areNullableKeysEqual(userKey.key, key));
    }

    public boolean bindKey(UUID playerId, @Nullable UUID issuingPlayerId, Ed25519PublicKeyParameters key) {
        final Boolean[] dirty = {false};

        userKeysMap.compute(playerId, (uuid, userKeys) -> {
            List<UserKey> newKeys = userKeys != null ? userKeys : new ArrayList<>();
            boolean found = newKeys.stream().anyMatch(entry -> KeyUtil.areNullableKeysEqual(entry.key, key));

            if (!found) {
                dirty[0] = true;

                UserKey userKey = new UserKey();
                userKey.key = key;
                userKey.issuingPlayer = issuingPlayerId;
                userKey.registrationTime = Instant.now();
                newKeys.add(userKey);

                if (issuingPlayerId == null) {
                    Constants.LOG.info("Key {} has been bound to {}.", Base64Util.encode(key.getEncoded()), playerId);
                } else {
                    Constants.LOG.info(
                            "Key {} has been bound by {} to {}.",
                            Base64Util.encode(key.getEncoded()),
                            issuingPlayerId,
                            playerId);
                }
            }

            return newKeys;
        });

        if (dirty[0]) {
            write();
        }

        return dirty[0];
    }

    public boolean unbindKey(UUID playerId, Ed25519PublicKeyParameters key) {
        final Boolean[] dirty = {false};

        userKeysMap.compute(playerId, (uuid, userKeys) -> {
            if (userKeys == null || userKeys.isEmpty()) {
                return null;
            }

            if (userKeys.removeIf(userKey -> KeyUtil.areNullableKeysEqual(userKey.key, key))) {
                dirty[0] = true;

                Constants.LOG.info("Key {} has been unbound from {}.", Base64Util.encode(key.getEncoded()), playerId);

                if (userKeys.isEmpty()) {
                    return null;
                }
            }

            return userKeys;
        });

        if (dirty[0]) {
            write();
        }

        return dirty[0];
    }

    public void read() {
        try {
            String json = Files.readString(AuthorisedKeysModCore.FILE_PATHS.AUTHORISED_KEYS_PATH);
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                    .create();
            List<UserJsonEntry> entries = gson.fromJson(json, new TypeToken<List<UserJsonEntry>>() {}.getType());

            userKeysMap.clear();
            entries.forEach(entry -> {
                List<UserKey> keys = entry.keys.stream()
                        .map(jsonEntry -> {
                            UserKey userKey = new UserKey();
                            userKey.key = new Ed25519PublicKeyParameters(Base64Util.decode(jsonEntry.key));
                            userKey.issuingPlayer = jsonEntry.issued_by;
                            userKey.registrationTime = jsonEntry.time_added;

                            return userKey;
                        })
                        .toList();

                userKeysMap.put(entry.user, keys);
            });
        } catch (FileNotFoundException ignored) {
            // Ignored.
        } catch (IOException e) {
            Constants.LOG.error("Could not load user entries list: {}", e.toString());
        }

        Constants.LOG.debug("Read {} user entries from disk.", userKeysMap.size());
    }

    public void write() {
        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.MOD_DIR);

            List<UserJsonEntry> out = new ArrayList<>();
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

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                    .setPrettyPrinting()
                    .create();
            Files.writeString(AuthorisedKeysModCore.FILE_PATHS.AUTHORISED_KEYS_PATH, gson.toJson(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} user entries to disk.", userKeysMap.size());
    }

    private static class UserKey {
        Ed25519PublicKeyParameters key;

        @Nullable
        UUID issuingPlayer;

        Instant registrationTime;
    }

    private record UserJsonEntry(UUID user, List<UserKeyJsonEntry> keys) {}

    private record UserKeyJsonEntry(String key, @Nullable UUID issued_by, Instant time_added) {}
}
