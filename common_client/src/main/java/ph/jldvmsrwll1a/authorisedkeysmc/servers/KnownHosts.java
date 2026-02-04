package ph.jldvmsrwll1a.authorisedkeysmc.servers;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AuthorisedKeysModCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.util.Base64Util;
import ph.jldvmsrwll1a.authorisedkeysmc.util.KeyUtil;

public class KnownHosts {
    private final ConcurrentHashMap<String, Ed25519PublicKeyParameters> knownHosts = new ConcurrentHashMap<>();

    public KnownHosts() {
        read();
    }

    public @Nullable Ed25519PublicKeyParameters getHostKey(String hostAddress) {
        return knownHosts.get(hostAddress);
    }

    public void setHostKey(String hostAddress, @Nullable Ed25519PublicKeyParameters key) {
        Ed25519PublicKeyParameters old = knownHosts.get(hostAddress);
        if (!KeyUtil.areNullableKeysEqual(old, key)) {
            if (key != null) {
                knownHosts.put(hostAddress, key);
            } else {
                knownHosts.remove(hostAddress);
            }

            write();
        }
    }

    public void read() {
        try {
            String json = Files.readString(AuthorisedKeysModCore.FILE_PATHS.KNOWN_HOSTS_PATH);
            Gson gson = new Gson();
            List<HostJsonEntry> entries = gson.fromJson(json, new TypeToken<ArrayList<HostJsonEntry>>() {}.getType());

            knownHosts.clear();
            entries.forEach(entry -> {
                Ed25519PublicKeyParameters key = new Ed25519PublicKeyParameters(Base64Util.decode(entry.host_key));
                knownHosts.put(entry.address, key);
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

            ArrayList<HostJsonEntry> out = new ArrayList<>();
            for (var entry : knownHosts.entrySet()) {
                Ed25519PublicKeyParameters key = entry.getValue();
                out.add(new HostJsonEntry(entry.getKey(), key));
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(AuthorisedKeysModCore.FILE_PATHS.KNOWN_HOSTS_PATH, gson.toJson(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} known hosts to disk.", knownHosts.size());
    }

    private record HostJsonEntry(String address, @Nullable String host_key) {
        private HostJsonEntry(String address, @Nullable Ed25519PublicKeyParameters host_key) {
            this(address, host_key == null ? null : Base64Util.encode(host_key.getEncoded()));
        }
    }
}
