package ph.jldvmsrwll1a.authorisedkeysmc.key;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkKeyPair;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPrivateKey;
import ph.jldvmsrwll1a.authorisedkeysmc.crypto.AkPublicKey;

/**
 * Memory-only private key cache.
 */
@NullMarked
public class PrivateKeyCache {
    private final ConcurrentHashMap<AkPublicKey, AkPrivateKey> CACHED_KEYS = new ConcurrentHashMap<>();

    /**
     * Insert the private key of the keypair in the cache.
     * @param keypair The keypair with the private key to cache.
     */
    public void cacheKey(AkKeyPair.Plain keypair) {
        CACHED_KEYS.put(keypair.getPublic(), keypair.getPrivateKey());
    }

    /**
     * Attempt to retrieve the cached decrypted private key.
     * @param keypair The keypair to "decrypt".
     * @return A decrypted key pair with the corresponding private key, if one was cached.
     */
    public Optional<AkKeyPair.Plain> decryptKeypair(AkKeyPair.Encrypted keypair) {
        AkPrivateKey secret = CACHED_KEYS.get(keypair.getPublic());

        if (secret != null) {
            return Optional.of(new AkKeyPair.Plain(keypair.getName(), Instant.now(), secret));
        } else {
            return Optional.empty();
        }
    }
}
