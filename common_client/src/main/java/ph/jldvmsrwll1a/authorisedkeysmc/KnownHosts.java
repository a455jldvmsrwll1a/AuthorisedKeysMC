package ph.jldvmsrwll1a.authorisedkeysmc;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;
import ph.jldvmsrwll1a.authorisedkeysmc.util.KeyUtil;

public class KnownHosts {
    private static final List<String> DEFAULT_KEY_LIST = List.of("default");

    private final ConcurrentHashMap<String, HostInfo> knownHosts = new ConcurrentHashMap<>();

    public KnownHosts() {
        read();
    }

    public @Nullable Ed25519PublicKeyParameters getHostKey(String hostAddress) {
        HostInfo info = knownHosts.get(hostAddress);

        return info == null ? null : info.hostKey;
    }

    public void setHostKey(String hostAddress, @Nullable Ed25519PublicKeyParameters key) {
        final Boolean[] dirty = {false};

        knownHosts.compute(hostAddress, (name, info) -> {
            HostInfo newInfo = info != null ? info : new HostInfo();

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

    public @NotNull List<String> getKeysUsedForHost(@Nullable String hostAddress) {
        if (hostAddress == null) {
            return DEFAULT_KEY_LIST;
        }

        HostInfo info = knownHosts.get(hostAddress);
        return (info == null || info.keysToUse.isEmpty()) ? DEFAULT_KEY_LIST : info.keysToUse;
    }

    public boolean addKeyForHost(String hostAddress, String keyName) {
        final Boolean[] dirty = {false};

        knownHosts.compute(hostAddress, (name, info) -> {
            if (info == null) {
                info = new HostInfo();
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

    public List<String> getHostsUsingKey(String keyName) {
        List<String> addresses = new ArrayList<>();

        knownHosts.forEach((address, info) -> {
            if (info.keysToUse.contains(keyName)) {
                addresses.add(address);
            }
        });

        return addresses;
    }

    public void read() {
        try {
            String json = Files.readString(AuthorisedKeysModCore.FILE_PATHS.KNOWN_HOSTS_PATH);
            Gson gson = new Gson();
            List<HostJsonEntry> entries = gson.fromJson(json, new TypeToken<List<HostJsonEntry>>() {}.getType());

            knownHosts.clear();
            entries.forEach(entry -> {
                HostInfo info = new HostInfo();
                info.hostKey = new Ed25519PublicKeyParameters(Base64Util.decode(entry.host_key));
                info.keysToUse =
                        entry.use_keys != null ? Arrays.stream(entry.use_keys).toList() : new ArrayList<>();

                knownHosts.put(entry.address, info);
            });
        } catch (FileNotFoundException ignored) {
            // Ignored.
        } catch (IOException e) {
            Constants.LOG.error("Could not load known hosts list: {}", e.toString());
        }

        Constants.LOG.debug("Read {} known hosts from disk.", knownHosts.size());
    }

    public void write() {
        try {
            Files.createDirectories(AuthorisedKeysModCore.FILE_PATHS.MOD_DIR);

            List<HostJsonEntry> out = new ArrayList<>();
            for (var entry : knownHosts.entrySet()) {
                HostInfo info = entry.getValue();
                out.add(new HostJsonEntry(entry.getKey(), info.hostKey, info.keysToUse));
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(AuthorisedKeysModCore.FILE_PATHS.KNOWN_HOSTS_PATH, gson.toJson(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} known hosts to disk.", knownHosts.size());
    }

    private static class HostInfo {
        private @Nullable Ed25519PublicKeyParameters hostKey;
        private @NotNull List<String> keysToUse = new ArrayList<>();
    }

    private record HostJsonEntry(
            String address,
            @Nullable String host_key,
            @Nullable String[] use_keys) {
        private HostJsonEntry(String address, @Nullable Ed25519PublicKeyParameters host_key, List<String> use_keys) {
            this(
                    address,
                    host_key == null ? null : Base64Util.encode(host_key.getEncoded()),
                    use_keys.toArray(new String[0]));
        }
    }
}
