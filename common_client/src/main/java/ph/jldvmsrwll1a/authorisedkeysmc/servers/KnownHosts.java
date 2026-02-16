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
import org.jspecify.annotations.Nullable;
import ph.jldvmsrwll1a.authorisedkeysmc.AkmcCore;
import ph.jldvmsrwll1a.authorisedkeysmc.Constants;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;

public class KnownHosts {
    private final ConcurrentHashMap<String, AkPublicKey> knownHosts = new ConcurrentHashMap<>();

    public KnownHosts() {
        read();
    }

    public @Nullable AkPublicKey getHostKey(String hostAddress) {
        return knownHosts.get(hostAddress);
    }

    public void setHostKey(String hostAddress, AkPublicKey newKey) {
        AkPublicKey currentKey = knownHosts.get(hostAddress);

        if (!newKey.equals(currentKey)) {
            knownHosts.put(hostAddress, newKey);

            write();
        }
    }

    public void clearHostKey(String hostAddress) {
        if (knownHosts.remove(hostAddress) != null) {
            write();
        }
    }

    public void read() {
        try {
            String json = Files.readString(AkmcCore.FILE_PATHS.KNOWN_HOSTS_PATH);
            Gson gson = new Gson();
            List<HostJsonEntry> entries = gson.fromJson(json, new TypeToken<ArrayList<HostJsonEntry>>() {}.getType());

            knownHosts.clear();
            entries.forEach(entry -> {
                AkPublicKey key = new AkPublicKey(entry.host_key);
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
            Files.createDirectories(AkmcCore.FILE_PATHS.MOD_DIR);

            ArrayList<HostJsonEntry> out = new ArrayList<>();
            for (var entry : knownHosts.entrySet()) {
                AkPublicKey key = entry.getValue();
                out.add(new HostJsonEntry(entry.getKey(), key));
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(AkmcCore.FILE_PATHS.KNOWN_HOSTS_PATH, gson.toJson(out));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Constants.LOG.debug("Wrote {} known hosts to disk.", knownHosts.size());
    }

    private record HostJsonEntry(String address, String host_key) {
        private HostJsonEntry(String address, AkPublicKey host_key) {
            this(address, host_key.toString());
        }
    }
}
