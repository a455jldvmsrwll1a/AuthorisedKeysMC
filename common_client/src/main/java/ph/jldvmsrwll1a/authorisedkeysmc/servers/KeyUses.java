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
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;

public final class KeyUses {
    private final ConcurrentHashMap<String, String> usedKeyTable = new ConcurrentHashMap<>();

    public KeyUses() {
        read();
    }

    public @Nullable String getKeyNameUsedForServer(@NonNull String name) {
        return usedKeyTable.get(name);
    }

    public void setKeyNameUsedForServer(String serverName, @Nullable String keyName) {
        if (keyName != null) {
            String oldKeyName = usedKeyTable.put(serverName, keyName);

            if (oldKeyName == null || !oldKeyName.equals(keyName)) {
                write();
            }
        } else {
            String oldKeyName = usedKeyTable.remove(serverName);

            if (oldKeyName != null) {
                write();
            }
        }
    }

    public ArrayList<String> getServersUsingKey(String keyName) {
        ArrayList<String> servers = new ArrayList<>();

        usedKeyTable.forEach((address, keys) -> {
            if (keys.equals(keyName)) {
                servers.add(address);
            }
        });

        servers.sort(String::compareToIgnoreCase);

        return servers;
    }

    public void read() {
        try {
            String json = Files.readString(AkmcCore.FILE_PATHS.KEY_USES_PATH);
            Gson gson = new Gson();
            List<ServerKeyListJsonEntry> entries =
                    gson.fromJson(json, new TypeToken<ArrayList<ServerKeyListJsonEntry>>() {}.getType());

            usedKeyTable.clear();
            entries.forEach(entry -> {
                if (entry.use_keys != null && entry.use_keys.length > 0 && entry.use_keys[0] != null) {
                    usedKeyTable.put(entry.name, entry.use_keys[0]);
                }
            });
        } catch (FileNotFoundException ignored) {
            // Ignored.
        } catch (IOException e) {
            Constants.LOG.error("Could not load known key use list: {}", e.toString());
        }
    }

    public void write() {
        try {
            Files.createDirectories(AkmcCore.FILE_PATHS.MOD_DIR);

            ArrayList<ServerKeyListJsonEntry> out = new ArrayList<>();
            for (var entry : usedKeyTable.entrySet()) {
                out.add(new ServerKeyListJsonEntry(entry.getKey(), entry.getValue()));
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(AkmcCore.FILE_PATHS.KEY_USES_PATH, gson.toJson(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record ServerKeyListJsonEntry(
            String name, @Nullable String[] use_keys) {
        private ServerKeyListJsonEntry(String name, String useKey) {
            this(name, new String[] {useKey});
        }
    }
}
