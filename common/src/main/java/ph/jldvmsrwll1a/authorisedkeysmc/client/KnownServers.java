package ph.jldvmsrwll1a.authorisedkeysmc.client;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;
import ph.jldvmsrwll1a.authorisedkeysmc.util.KeyUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class KnownServers {
    private static final List<String> DEFAULT_KEY_LIST = List.of("default");

    private final ConcurrentHashMap<String, ServerInfo> knownServers = new ConcurrentHashMap<>();

    public KnownServers() {
        read();
    }

    public @Nullable Ed25519PublicKeyParameters getServerKey(String server) {
        ServerInfo info = knownServers.get(server);

        return info == null ? null : info.hostKey;
    }

    public void setServerKey(String server, @Nullable Ed25519PublicKeyParameters key) {
        final Boolean[] dirty = {false};

        knownServers.compute(server, (name, info) -> {
            ServerInfo newInfo = info != null ? info : new ServerInfo();

            if (!KeyUtil.areNullableKeysEqual(newInfo.hostKey, key)) {
                dirty[0] = true;
            }

            newInfo.hostKey = key;
            return newInfo;
        });

        if (dirty[0]) {
            write();
        }
    }

    public @NotNull List<String> getKeysForServer(@Nullable String server) {
        if (server == null) {
            return DEFAULT_KEY_LIST;
        }

        ServerInfo info = knownServers.get(server);
        return (info == null || info.keysToUse.isEmpty()) ? DEFAULT_KEY_LIST : info.keysToUse;
    }

    public boolean addKeyForServer(String server, String keyName) {
        final Boolean[] dirty = {false};

        knownServers.compute(server, (name, info) -> {
            if (info == null) {
                info = new ServerInfo();
            }

            if (info.keysToUse.stream().noneMatch(key -> key.equals(keyName))) {
                dirty[0] = true;
            }

            info.keysToUse.add(keyName);
            return info;
        });

        if (dirty[0]) {
            write();
        }

        return dirty[0];
    }

    public void read() {
        try {
            String json = Files.readString(AuthorisedKeysModCore.FILE_PATHS.KNOWN_SERVERS_PATH);
            Gson gson = new Gson();
            List<ServerJsonEntry> entries = gson.fromJson(json, new TypeToken<List<ServerJsonEntry>>() {}.getType());

            knownServers.clear();
            entries.forEach(entry -> {
                ServerInfo info = new ServerInfo();
                info.hostKey = new Ed25519PublicKeyParameters(Base64Util.decode(entry.host_key));
                info.keysToUse = entry.use_keys != null ? Arrays.stream(entry.use_keys).toList() : new ArrayList<>();

                knownServers.put(entry.name, info);
            });
        } catch (FileNotFoundException ignored) {
            // Ignored.
        } catch (IOException e) {
            Constants.LOG.error("Could not load known servers list: {}", e.toString());
        }

        Constants.LOG.debug("Read {} known servers from disk.", knownServers.size());
    }

    public void write() {
        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.MOD_DIR);

            List<ServerJsonEntry> out = new ArrayList<>();
            for (var entry : knownServers.entrySet()) {
                ServerInfo info = entry.getValue();
                out.add(new ServerJsonEntry(entry.getKey(), info.hostKey, info.keysToUse));
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(AuthorisedKeysModCore.FILE_PATHS.KNOWN_SERVERS_PATH, gson.toJson(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} known servers to disk.", knownServers.size());
    }

    private static class ServerInfo {
        private @Nullable Ed25519PublicKeyParameters hostKey;
        private @NotNull List<String> keysToUse = new ArrayList<>();
    }

    private record ServerJsonEntry(String name, @Nullable String host_key, @Nullable String[] use_keys) {
        private ServerJsonEntry(String name, @Nullable Ed25519PublicKeyParameters host_key, List<String> use_keys) {
            this(name, host_key == null ? null : Base64Util.encode(host_key.getEncoded()), use_keys.toArray(new String[0]));
        }
    }
}
