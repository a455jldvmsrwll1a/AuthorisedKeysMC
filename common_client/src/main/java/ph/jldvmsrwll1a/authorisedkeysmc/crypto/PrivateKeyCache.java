package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;

/**
 * Memory-only private key cache.
 */
public class PrivateKeyCache {
    private final ConcurrentHashMap<AkPublicKey, AkPrivateKey> CACHED_KEYS = new ConcurrentHashMap<>();

    /**
     * Insert the private key of the keypair in the cache.
     * @param keypair The keypair with the private key to cache. Must already be decrypted.
     */
    public void cacheKey(@NonNull AkKeyPair keypair) {
        try {
            CACHED_KEYS.put(keypair.getPublic(), keypair.getDecryptedPrivate());
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("invalid keypair provided", e);
        }
    }

    /**
     * Attempt to retrieve the cached decrypted private key (if existing) and update {@code keypair} with it.
     * @param keypair The keypair to "decrypt". The public key must be known.
     * @return {@code true} if it was able to retrieve the private key, or it was already decrypted in the first place. Otherwise, returns {@code false} and leaves {@code keypair} unmodified.
     */
    public boolean decryptKeypair(@NonNull AkKeyPair keypair) {
        if (!keypair.requiresDecryption()) {
            return true;
        }

        try {
            AkPrivateKey secret = CACHED_KEYS.get(keypair.getPublic());

            if (secret != null) {
                keypair.setPrivateKey(secret);

                return true;
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("invalid keypair provided", e);
        }

        return false;
    }
}
