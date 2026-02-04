package ph.jldvmsrwll1a.authorisedkeysmc.crypto;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Memory-only private key cache.
 */
public class PrivateKeyCache {
    private final ConcurrentHashMap<HashablePublicKey, Ed25519PrivateKeyParameters> CACHED_KEYS =
            new ConcurrentHashMap<>();

    /**
     * Insert the private key of the keypair in the cache.
     * @param keypair The keypair with the private key to cache. Must already be decrypted.
     */
    public void cacheKey(@NonNull LoadedKeypair keypair) {
        try {
            CACHED_KEYS.put(new HashablePublicKey(keypair.getPublic()), keypair.getDecryptedPrivate());
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("invalid keypair provided", e);
        }
    }

    /**
     * Attempt to retrieve the cached decrypted private key (if existing) and update {@code keypair} with it.
     * @param keypair The keypair to "decrypt". The public key must be known.
     * @return {@code true} if it was able to retrieve the private key, or it was already decrypted in the first place. Otherwise, returns {@code false} and leaves {@code keypair} unmodified.
     */
    public boolean decryptKeypair(@NonNull LoadedKeypair keypair) {
        if (!keypair.requiresDecryption()) {
            return true;
        }

        try {
            Ed25519PrivateKeyParameters secret = CACHED_KEYS.get(new HashablePublicKey(keypair.getPublic()));

            if (secret != null) {
                keypair.setPrivateKey(secret);

                return true;
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("invalid keypair provided", e);
        }

        return false;
    }

    /// The usual public/private key params classes used do not provide consistent hash codes
    /// and doesn't check for equality by their value.
    private static final class HashablePublicKey {
        public final byte @NonNull [] bytes;
        public final int hash;

        private HashablePublicKey(@NonNull Ed25519PublicKeyParameters pubKey) {
            bytes = pubKey.getEncoded();
            hash = Arrays.hashCode(bytes);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof HashablePublicKey key) {
                return hash == key.hash && Arrays.equals(bytes, key.bytes);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
