package ph.jldvmsrwll1a.authorisedkeysmc.servers;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public final class KeyUses {
    private static final ArrayList<String> DEFAULT_KEY_LIST = new ArrayList<>(List.of("default"));

    private final ConcurrentHashMap<String, ArrayList<String>> usedKeysTable = new ConcurrentHashMap<>();

    public KeyUses() {
        read();
    }

    public List<String> getServers() {
        return Collections.list(usedKeysTable.keys());
    }

    public @NonNull ArrayList<String> getKeysUsedForServer(@Nullable String name) {
        if (name == null) {
            return DEFAULT_KEY_LIST;
        }

        ArrayList<String> keys = usedKeysTable.get(name);
        return keys != null ? new ArrayList<>(keys) : DEFAULT_KEY_LIST;
    }

    public boolean addKeyForServer(String hostAddress, String keyName) {
        final Boolean[] dirty = {false};

        usedKeysTable.compute(hostAddress, (name, keys) -> {
            if (keys == null) {
                keys = new ArrayList<>(1);
            }

            if (keys.stream().noneMatch(key -> key.equals(keyName))) {
                dirty[0] = true;
            }

            keys.add(keyName);

            return keys;
        });

        if (dirty[0]) {
            write();
        }

        return dirty[0];
    }

    public ArrayList<String> getServersUsingKey(String keyName) {
        ArrayList<String> servers = new ArrayList<>();

        usedKeysTable.forEach((address, keys) -> {
            if (keys.contains(keyName)) {
                servers.add(address);
            }
        });

        servers.sort(String::compareToIgnoreCase);

        return servers;
    }

    public void read() {
        try {
            String json = Files.readString(AuthorisedKeysModCore.FILE_PATHS.KEY_USES_PATH);
            Gson gson = new Gson();
            List<ServerKeyListJsonEntry> entries =
                    gson.fromJson(json, new TypeToken<ArrayList<ServerKeyListJsonEntry>>() {}.getType());

            usedKeysTable.clear();
            entries.forEach(entry -> {
                ArrayList<String> keys = entry.use_keys != null
                        ? new ArrayList<>(Arrays.stream(entry.use_keys).toList())
                        : new ArrayList<>();

                usedKeysTable.put(entry.name, keys);
            });
        } catch (FileNotFoundException ignored) {
            // Ignored.
        } catch (IOException e) {
            Constants.LOG.error("Could not load known key use list: {}", e.toString());
        }
    }

    public void write() {
        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.MOD_DIR);

            ArrayList<ServerKeyListJsonEntry> out = new ArrayList<>();
            for (var entry : usedKeysTable.entrySet()) {
                ArrayList<String> keys = entry.getValue();
                out.add(new ServerKeyListJsonEntry(entry.getKey(), keys));
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(AuthorisedKeysModCore.FILE_PATHS.KEY_USES_PATH, gson.toJson(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record ServerKeyListJsonEntry(
            String name, @Nullable String[] use_keys) {
        private ServerKeyListJsonEntry(String name, ArrayList<String> use_keys) {
            this(name, use_keys.toArray(new String[0]));
        }
    }
}
